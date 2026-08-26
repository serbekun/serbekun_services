package com.serbekun.ss.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.BuildInfo;
import com.serbekun.ss.config.Config;
import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo;
import com.serbekun.ss.repo.shorturl.ShortUrlRepo;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesRepo;
import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.EndpointRegistry;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.encoding.EncodingService;
import com.serbekun.ss.service.hash.HashService;
import com.serbekun.ss.service.id.IdService;
import com.serbekun.ss.service.json.JsonService;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;
import com.serbekun.ss.service.qr.QrService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.shorturl.ShortUrlService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.Youtube;
import com.serbekun.ss.service.youtube.YoutubeDomains;
import com.serbekun.ss.service.youtube.YoutubeService;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests: the full route tree from {@link ServerFactory} wired
 * with real services on top of in-memory repositories and a temp raw-files dir.
 * Only the yt-dlp binary ({@link Youtube}) is mocked.
 */
class ServerHttpIntegrationTest {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private Javalin app;
    private Youtube youtube;

    @BeforeEach
    void setUp() {
        Config config = new Config(0, 20, null, 30, "yt-dlp", "deno",
            "repository", "repository/repositories/links_repositories.json",
            "repository/endpoint_access_tokens.json", "repository/uploaded_files_raw/",
            "repository/uploaded_files/uploaded_files.json", "repository/short_url/short_url.json",
            "repository/www.youtube.com_cookies.txt");

        var linkRepo = new LinkRepositoryRepo(new HashMap<>());
        var tokensRepo = new EndpointsAccessTokensRepo(new HashMap<>());
        var uploadedRepo = new UploadedFilesRepo(new HashMap<>());
        var shortUrlRepo = new ShortUrlRepo(new HashMap<>());

        var endpointRegistry = new EndpointRegistry();
        var authService = new AuthService(tokensRepo, endpointRegistry);
        var linkService = new LinkRepositoryService(linkRepo);
        var shortUrlService = new ShortUrlService(shortUrlRepo);
        var uploadedService = new UploadedFilesService(uploadedRepo, tempDir.resolve("raw"));

        youtube = mock(Youtube.class);
        var youtubeService = new YoutubeService(
            youtube, new YoutubeDomains(Set.of("youtube.com", "youtu.be"), Set.of()));

        var loader = new ResourceLoader();
        var resourcesService = new ResourcesService(loader, new ResourceCache(loader));

        app = ServerFactory.create(config, resourcesService, linkService,
            new CipherService(), new HashService(), new QrService(), new EncodingService(), new IdService(), new JsonService(), youtubeService, uploadedService,
            shortUrlService, authService, endpointRegistry);
    }

    private static RequestBody jsonBody(String json) {
        return RequestBody.create(json, JSON);
    }

    private static JsonNode json(Response response) throws Exception {
        return mapper.readTree(response.body().string());
    }

    // region index + static

    @Test
    void indexPageIsServed() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/")) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string().toLowerCase()).contains("<html");
            }
        });
    }

    @Test
    void staticHtmlIsServedAndUnknownNameIs404() {
        JavalinTest.test(app, (server, client) -> {
            try (Response ok = client.get("/static/v0/html/index.html")) {
                assertThat(ok.code()).isEqualTo(200);
                assertThat(ok.header("Content-Type")).contains("text/html");
            }
            try (Response missing = client.get("/static/v0/html/no-such-page.html")) {
                assertThat(missing.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void staticHtmlListingReturnsJsonArray() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/static/v0/html")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode listing = json(response);
                assertThat(listing.isArray()).isTrue();
                assertThat(listing.toString()).contains("index.html");
            }
        });
    }

    @Test
    void staticCssIsServed() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/static/v0/css/shared.css")) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.header("Content-Type")).contains("text/css");
            }
        });
    }

    // endregion

    // region version

    @Test
    void versionEndpointReturnsBuildVersion() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/version")) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("version").asText()).isEqualTo(BuildInfo.version());
            }
        });
    }

    // endregion

    // region cipher

    @Test
    void cipherKeyGenerationAndRoundtripOverHttp() {
        JavalinTest.test(app, (server, client) -> {
            String key;
            try (Response keyResponse = client.get("/api/v0/cipher/aes")) {
                assertThat(keyResponse.code()).isEqualTo(200);
                key = json(keyResponse).get("key").asText();
                assertThat(key).isNotBlank();
            }

            String data = Base64.getEncoder()
                .encodeToString("integration secret".getBytes(StandardCharsets.UTF_8));

            String encrypted;
            try (Response encryptResponse = client.request("/api/v0/cipher/aes/encrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + data + "\",\"key\":\"" + key + "\"}")))) {
                assertThat(encryptResponse.code()).isEqualTo(200);
                encrypted = json(encryptResponse).get("data").asText();
            }

            try (Response decryptResponse = client.request("/api/v0/cipher/aes/decrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + encrypted + "\",\"key\":\"" + key + "\"}")))) {
                assertThat(decryptResponse.code()).isEqualTo(200);
                assertThat(json(decryptResponse).get("data").asText()).isEqualTo(data);
            }
        });
    }

    @Test
    void cipherEncryptRejectsMissingFields() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/cipher/aes/encrypt",
                    b -> b.post(jsonBody("{\"data\":\"\",\"key\":\"\"}")))) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    void rsaKeyPairEncryptDecryptRoundtripOverHttp() {
        JavalinTest.test(app, (server, client) -> {
            String publicKey;
            String privateKey;
            try (Response keyResponse = client.get("/api/v0/cipher/rsa/keypair")) {
                assertThat(keyResponse.code()).isEqualTo(200);
                JsonNode body = json(keyResponse);
                publicKey = body.get("publicKey").asText();
                privateKey = body.get("privateKey").asText();
                assertThat(publicKey).isNotBlank();
                assertThat(privateKey).isNotBlank();
            }

            String data = Base64.getEncoder()
                .encodeToString("rsa integration secret".getBytes(StandardCharsets.UTF_8));

            String encrypted;
            try (Response encryptResponse = client.request("/api/v0/cipher/rsa/encrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + data + "\",\"publicKey\":\"" + publicKey + "\"}")))) {
                assertThat(encryptResponse.code()).isEqualTo(200);
                encrypted = json(encryptResponse).get("data").asText();
            }

            try (Response decryptResponse = client.request("/api/v0/cipher/rsa/decrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + encrypted + "\",\"privateKey\":\"" + privateKey + "\"}")))) {
                assertThat(decryptResponse.code()).isEqualTo(200);
                assertThat(json(decryptResponse).get("data").asText()).isEqualTo(data);
            }
        });
    }

    @Test
    void rsaSignAndVerifyOverHttp() {
        JavalinTest.test(app, (server, client) -> {
            String publicKey;
            String privateKey;
            try (Response keyResponse = client.get("/api/v0/cipher/rsa/keypair")) {
                JsonNode body = json(keyResponse);
                publicKey = body.get("publicKey").asText();
                privateKey = body.get("privateKey").asText();
            }

            String data = Base64.getEncoder()
                .encodeToString("sign this".getBytes(StandardCharsets.UTF_8));
            String otherData = Base64.getEncoder()
                .encodeToString("not this".getBytes(StandardCharsets.UTF_8));

            String signature;
            try (Response signResponse = client.request("/api/v0/cipher/rsa/sign",
                    b -> b.post(jsonBody("{\"data\":\"" + data + "\",\"privateKey\":\"" + privateKey + "\"}")))) {
                assertThat(signResponse.code()).isEqualTo(200);
                signature = json(signResponse).get("signature").asText();
                assertThat(signature).isNotBlank();
            }

            try (Response verifyResponse = client.request("/api/v0/cipher/rsa/verify",
                    b -> b.post(jsonBody("{\"data\":\"" + data + "\",\"signature\":\"" + signature
                        + "\",\"publicKey\":\"" + publicKey + "\"}")))) {
                assertThat(verifyResponse.code()).isEqualTo(200);
                assertThat(json(verifyResponse).get("valid").asBoolean()).isTrue();
            }

            // A signature that does not match is a 200 with valid=false, not an error.
            try (Response verifyResponse = client.request("/api/v0/cipher/rsa/verify",
                    b -> b.post(jsonBody("{\"data\":\"" + otherData + "\",\"signature\":\"" + signature
                        + "\",\"publicKey\":\"" + publicKey + "\"}")))) {
                assertThat(verifyResponse.code()).isEqualTo(200);
                assertThat(json(verifyResponse).get("valid").asBoolean()).isFalse();
            }
        });
    }

    @Test
    void rsaEncryptRejectsMalformedKey() {
        JavalinTest.test(app, (server, client) -> {
            String data = Base64.getEncoder().encodeToString("x".getBytes(StandardCharsets.UTF_8));
            String notAKey = Base64.getEncoder().encodeToString("nope".getBytes(StandardCharsets.UTF_8));

            try (Response response = client.request("/api/v0/cipher/rsa/encrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + data + "\",\"publicKey\":\"" + notAKey + "\"}")))) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    void rsaEncryptRejectsPayloadLargerThanTheKey() {
        JavalinTest.test(app, (server, client) -> {
            String publicKey;
            try (Response keyResponse = client.get("/api/v0/cipher/rsa/keypair")) {
                publicKey = json(keyResponse).get("publicKey").asText();
            }

            // Past what OAEP-SHA256 leaves room for in a 2048-bit key.
            String tooLarge = Base64.getEncoder().encodeToString(new byte[512]);

            try (Response response = client.request("/api/v0/cipher/rsa/encrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + tooLarge + "\",\"publicKey\":\"" + publicKey + "\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("hybrid");
            }
        });
    }

    @Test
    void rsaSignRejectsMissingFields() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/cipher/rsa/sign",
                    b -> b.post(jsonBody("{\"data\":\"\",\"privateKey\":\"\"}")))) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    void hybridEncryptDecryptRoundtripOverHttp() {
        JavalinTest.test(app, (server, client) -> {
            String publicKey;
            String privateKey;
            try (Response keyResponse = client.get("/api/v0/cipher/rsa/keypair")) {
                JsonNode body = json(keyResponse);
                publicKey = body.get("publicKey").asText();
                privateKey = body.get("privateKey").asText();
            }

            // Comfortably past what a 2048-bit RSA key could encrypt on its own.
            String data = Base64.getEncoder().encodeToString(new byte[8192]);

            String encrypted;
            String encryptedKey;
            try (Response encryptResponse = client.request("/api/v0/cipher/hybrid/encrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + data + "\",\"publicKey\":\"" + publicKey + "\"}")))) {
                assertThat(encryptResponse.code()).isEqualTo(200);
                JsonNode body = json(encryptResponse);
                encrypted = body.get("data").asText();
                encryptedKey = body.get("encryptedKey").asText();
                assertThat(encryptedKey).isNotBlank();
            }

            try (Response decryptResponse = client.request("/api/v0/cipher/hybrid/decrypt",
                    b -> b.post(jsonBody("{\"data\":\"" + encrypted + "\",\"encryptedKey\":\"" + encryptedKey
                        + "\",\"privateKey\":\"" + privateKey + "\"}")))) {
                assertThat(decryptResponse.code()).isEqualTo(200);
                assertThat(json(decryptResponse).get("data").asText()).isEqualTo(data);
            }
        });
    }

    @Test
    void hybridDecryptRejectsMissingFields() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/cipher/hybrid/decrypt",
                    b -> b.post(jsonBody("{\"data\":\"abc\",\"encryptedKey\":\"\",\"privateKey\":\"\"}")))) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    // endregion

    // region hash

    @Test
    void hashReturnsKnownDigestForPlainText() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"hello\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("algorithm").asText()).isEqualTo("sha256");
                assertThat(body.get("hash").asText())
                    .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
                assertThat(body.get("bytes").asLong()).isEqualTo(5);
            }
        });
    }

    @Test
    void hashSupportsEveryAdvertisedAlgorithm() {
        JavalinTest.test(app, (server, client) -> {
            record Case(String algorithm, String body, String expected) {
            }

            var cases = java.util.List.of(
                new Case("sha256", "{\"algorithm\":\"sha256\",\"data\":\"hello\"}",
                    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"),
                new Case("sha512", "{\"algorithm\":\"sha512\",\"data\":\"hello\"}",
                    "9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca7"
                        + "2323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043"),
                new Case("blake3", "{\"algorithm\":\"blake3\",\"data\":\"hello\"}",
                    "ea8f163db38682925e4491c5e58d4bb3506ef8c14eb78a86e908c5624a67200f"),
                new Case("hmac-sha256", "{\"algorithm\":\"hmac-sha256\",\"data\":\"hello\",\"key\":\"secret\"}",
                    "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b"));

            for (Case testCase : cases) {
                try (Response response = client.request("/api/v0/hash", b -> b.post(jsonBody(testCase.body())))) {
                    assertThat(response.code()).as(testCase.algorithm()).isEqualTo(200);
                    JsonNode body = json(response);
                    assertThat(body.get("algorithm").asText()).isEqualTo(testCase.algorithm());
                    assertThat(body.get("hash").asText()).as(testCase.algorithm()).isEqualTo(testCase.expected());
                }
            }
        });
    }

    @Test
    void hashReadsBase64AndHexEncodedData() {
        JavalinTest.test(app, (server, client) -> {
            String sha256OfHello = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

            // "hello" spelled three ways must land on the same digest.
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"aGVsbG8=\",\"encoding\":\"base64\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("hash").asText()).isEqualTo(sha256OfHello);
            }
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"68656c6c6f\",\"encoding\":\"hex\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("hash").asText()).isEqualTo(sha256OfHello);
            }
        });
    }

    @Test
    void hashOfAnEmptyStringIsTheEmptyDigest() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("hash").asText())
                    .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                assertThat(body.get("bytes").asLong()).isZero();
            }
        });
    }

    @Test
    void hashRejectsUnknownAlgorithmAndMissingData() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"md5\",\"data\":\"hello\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                String body = response.body().string();
                assertThat(body).contains("md5").contains("sha256").contains("blake3");
            }
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("data is required");
            }
        });
    }

    @Test
    void hmacWithoutAKeyIsRejected() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"hmac-sha256\",\"data\":\"hello\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("key is required");
            }
        });
    }

    @Test
    void hashRejectsBadEncodingAndUndecodableData() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"hello\",\"encoding\":\"rot13\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("rot13");
            }
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"zzz!\",\"encoding\":\"base64\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("data");
            }
        });
    }

    @Test
    void hashFileStreamsAnUploadAndReportsItsName() {
        JavalinTest.test(app, (server, client) -> {
            MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("algorithm", "sha256")
                .addFormDataPart("file", "notes.txt",
                    RequestBody.create("hello".getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("text/plain")))
                .build();

            try (Response response = client.request("/api/v0/hash/file", b -> b.post(body))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode json = json(response);
                assertThat(json.get("hash").asText())
                    .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
                assertThat(json.get("name").asText()).isEqualTo("notes.txt");
                assertThat(json.get("bytes").asLong()).isEqualTo(5);
                assertThat(json.get("algorithm").asText()).isEqualTo("sha256");
            }
        });
    }

    @Test
    void hashFileDefaultsToSha256AndAcceptsOtherAlgorithms() {
        JavalinTest.test(app, (server, client) -> {
            MultipartBody noAlgorithm = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "a.bin",
                    RequestBody.create("hello".getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/octet-stream")))
                .build();

            try (Response response = client.request("/api/v0/hash/file", b -> b.post(noAlgorithm))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("algorithm").asText()).isEqualTo("sha256");
            }

            MultipartBody blake3 = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("algorithm", "blake3")
                .addFormDataPart("file", "a.bin",
                    RequestBody.create("hello".getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/octet-stream")))
                .build();

            try (Response response = client.request("/api/v0/hash/file", b -> b.post(blake3))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("hash").asText())
                    .isEqualTo("ea8f163db38682925e4491c5e58d4bb3506ef8c14eb78a86e908c5624a67200f");
            }
        });
    }

    @Test
    void hashFileRequiresAFile() {
        JavalinTest.test(app, (server, client) -> {
            MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("algorithm", "sha256")
                .build();

            try (Response response = client.request("/api/v0/hash/file", b -> b.post(body))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("file");
            }
        });
    }

    @Test
    void hashFileMatchesTheInMemoryEndpointForALargePayload() {
        JavalinTest.test(app, (server, client) -> {
            // Bigger than the streaming buffer, so the chunked path is what runs.
            byte[] payload = new byte[300_000];
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (i % 251);
            }

            String viaJson;
            try (Response response = client.request("/api/v0/hash",
                    b -> b.post(jsonBody("{\"algorithm\":\"blake3\",\"data\":\""
                        + Base64.getEncoder().encodeToString(payload) + "\",\"encoding\":\"base64\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                viaJson = json(response).get("hash").asText();
            }

            MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("algorithm", "blake3")
                .addFormDataPart("file", "big.bin",
                    RequestBody.create(payload, MediaType.parse("application/octet-stream")))
                .build();

            try (Response response = client.request("/api/v0/hash/file", b -> b.post(body))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode json = json(response);
                assertThat(json.get("hash").asText()).isEqualTo(viaJson);
                assertThat(json.get("bytes").asLong()).isEqualTo(payload.length);
            }
        });
    }

    @Test
    void verifyAcceptsAMatchingDigestAndRejectsAlteredData() {
        JavalinTest.test(app, (server, client) -> {
            String sha256OfHello = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

            try (Response response = client.request("/api/v0/hash/verify",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"hello\",\"hash\":\""
                        + sha256OfHello + "\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode json = json(response);
                assertThat(json.get("valid").asBoolean()).isTrue();
                assertThat(json.get("expected").asText()).isEqualTo(sha256OfHello);
                assertThat(json.get("actual").asText()).isEqualTo(sha256OfHello);
            }

            // A mismatch is a 200 with valid=false, not an error.
            try (Response response = client.request("/api/v0/hash/verify",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"hello!\",\"hash\":\""
                        + sha256OfHello + "\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode json = json(response);
                assertThat(json.get("valid").asBoolean()).isFalse();
                assertThat(json.get("actual").asText()).isNotEqualTo(sha256OfHello);
            }
        });
    }

    @Test
    void verifyRejectsANonHexHash() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/hash/verify",
                    b -> b.post(jsonBody("{\"algorithm\":\"sha256\",\"data\":\"hello\",\"hash\":\"nothex\"}")))) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("hex");
            }
        });
    }

    @Test
    void verifyWorksForKeyedHmac() {
        JavalinTest.test(app, (server, client) -> {
            String hmac = "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b";

            try (Response response = client.request("/api/v0/hash/verify",
                    b -> b.post(jsonBody("{\"algorithm\":\"hmac-sha256\",\"data\":\"hello\","
                        + "\"key\":\"secret\",\"hash\":\"" + hmac + "\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("valid").asBoolean()).isTrue();
            }

            try (Response response = client.request("/api/v0/hash/verify",
                    b -> b.post(jsonBody("{\"algorithm\":\"hmac-sha256\",\"data\":\"hello\","
                        + "\"key\":\"wrong\",\"hash\":\"" + hmac + "\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("valid").asBoolean()).isFalse();
            }
        });
    }

    // endregion

    // region short url

    @Test
    void shortUrlLifecycle() {
        JavalinTest.test(app, (server, client) -> {
            // create
            String id;
            String token;
            try (Response created = client.request("/api/v0/short-url",
                    b -> b.post(jsonBody("{\"url\":\"https://example.com/target\"}")))) {
                assertThat(created.code()).isEqualTo(201);
                JsonNode body = json(created);
                id = body.get("id").asText();
                token = body.get("token").asText();
                assertThat(id).matches("[A-Za-z0-9]{8}");
            }

            // redirect (without following it)
            OkHttpClient noRedirect = new OkHttpClient.Builder().followRedirects(false).build();
            Request redirectRequest = new Request.Builder()
                .url(client.getOrigin() + "/api/v0/short-url/" + id).build();
            try (Response redirect = noRedirect.newCall(redirectRequest).execute()) {
                assertThat(redirect.code()).isEqualTo(302);
                assertThat(redirect.header("Location")).isEqualTo("https://example.com/target");
            }

            // delete with wrong token
            try (Response forbidden = client.delete("/api/v0/short-url/" + id + "?token=wrong")) {
                assertThat(forbidden.code()).isEqualTo(403);
            }

            // delete with valid token
            try (Response deleted = client.delete("/api/v0/short-url/" + id + "?token=" + token)) {
                assertThat(deleted.code()).isEqualTo(204);
            }

            // now gone
            try (Response gone = noRedirect.newCall(redirectRequest).execute()) {
                assertThat(gone.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void shortUrlCreateRequiresUrl() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/short-url",
                    b -> b.post(jsonBody("{\"name\":\"no url\"}")))) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    void unknownShortUrlReturns404() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/short-url/zzzzzzzz")) {
                assertThat(response.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void shortUrlQrCreatesTheLinkAndACodeThatPointsAtIt() {
        JavalinTest.test(app, (server, client) -> {
            String id;
            String token;
            String shortUrl;
            String qr;
            try (Response created = client.request("/api/v0/short-url/qr",
                    b -> b.post(jsonBody("{\"url\":\"https://example.com/target\","
                        + "\"baseUrl\":\"https://ss.serbekun.com/\",\"size\":320}")))) {
                assertThat(created.code()).isEqualTo(201);
                JsonNode body = json(created);
                id = body.get("id").asText();
                token = body.get("token").asText();
                shortUrl = body.get("shortUrl").asText();
                qr = body.get("qr").asText();

                assertThat(id).matches("[A-Za-z0-9]{8}");
                assertThat(shortUrl).isEqualTo("https://ss.serbekun.com/api/v0/short-url/" + id);
                assertThat(body.get("format").asText()).isEqualTo("png");
                assertThat(body.get("contentType").asText()).isEqualTo("image/png");
                assertThat(body.get("size").asInt()).isEqualTo(320);
                assertThat(qr).startsWith("data:image/png;base64,");
            }

            // The code is only useful if it scans back to the short link.
            try (Response read = client.request("/api/v0/qr/read",
                    b -> b.post(jsonBody(mapper.createObjectNode().put("image", qr).toString())))) {
                assertThat(read.code()).isEqualTo(200);
                assertThat(json(read).get("text").asText()).isEqualTo(shortUrl);
            }

            // And the record behind it is an ordinary short url.
            try (Response deleted = client.delete("/api/v0/short-url/" + id + "?token=" + token)) {
                assertThat(deleted.code()).isEqualTo(204);
            }
        });
    }

    @Test
    void shortUrlQrDerivesTheLinkFromTheRequestHost() {
        JavalinTest.test(app, (server, client) -> {
            try (Response created = client.request("/api/v0/short-url/qr",
                    b -> b.post(jsonBody("{\"url\":\"https://example.com/target\"}")))) {
                assertThat(created.code()).isEqualTo(201);
                assertThat(json(created).get("shortUrl").asText())
                    .startsWith(client.getOrigin() + "/api/v0/short-url/");
            }
        });
    }

    @Test
    void shortUrlQrRejectsABadRequestWithoutCreatingAnything() {
        JavalinTest.test(app, (server, client) -> {
            try (Response noUrl = client.request("/api/v0/short-url/qr",
                    b -> b.post(jsonBody("{\"size\":512}")))) {
                assertThat(noUrl.code()).isEqualTo(400);
            }
            try (Response badSize = client.request("/api/v0/short-url/qr",
                    b -> b.post(jsonBody("{\"url\":\"https://example.com\",\"size\":4}")))) {
                assertThat(badSize.code()).isEqualTo(400);
                assertThat(json(badSize).get("error").asText()).contains("size must be between");
            }
            try (Response badBase = client.request("/api/v0/short-url/qr",
                    b -> b.post(jsonBody("{\"url\":\"https://example.com\",\"baseUrl\":\"ss.serbekun.com\"}")))) {
                assertThat(badBase.code()).isEqualTo(400);
            }
        });
    }

    // endregion

    // region qr

    @Test
    void qrGenerateReturnsAPngThatReadsBackAsThePayload() {
        JavalinTest.test(app, (server, client) -> {
            byte[] png;
            try (Response response = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"data\":\"https://ss.serbekun.com\",\"size\":256}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.header("Content-Type")).contains("image/png");
                assertThat(response.header("Content-Disposition")).contains("qr.png");
                png = response.body().bytes();
            }
            assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');

            MultipartBody upload = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "qr.png", RequestBody.create(png, MediaType.parse("image/png")))
                .build();

            try (Response read = client.request("/api/v0/qr/read", b -> b.post(upload))) {
                assertThat(read.code()).isEqualTo(200);
                JsonNode body = json(read);
                assertThat(body.get("found").asBoolean()).isTrue();
                assertThat(body.get("text").asText()).isEqualTo("https://ss.serbekun.com");
                assertThat(body.get("format").asText()).isEqualTo("QR_CODE");
            }
        });
    }

    @Test
    void qrGenerateReturnsSvgWhenAsked() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"data\":\"https://ss.serbekun.com\",\"format\":\"svg\","
                        + "\"size\":512,\"errorCorrection\":\"H\","
                        + "\"foreground\":\"#123456\",\"background\":\"#ffffff\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.header("Content-Type")).contains("image/svg+xml");
                assertThat(response.body().string())
                    .startsWith("<svg")
                    .contains("width=\"512\" height=\"512\"")
                    .contains("fill=\"#123456\"");
            }
        });
    }

    @Test
    void qrGenerateWrapsTheCodeInJsonWhenTheCallerAsksForIt() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"data\":\"embed me\"}"))
                          .header("Accept", "application/json"))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.header("Content-Type")).contains("application/json");
                JsonNode body = json(response);
                assertThat(body.get("format").asText()).isEqualTo("png");
                assertThat(body.get("contentType").asText()).isEqualTo("image/png");
                assertThat(body.get("size").asInt()).isEqualTo(512);
                assertThat(body.get("image").asText()).startsWith("data:image/png;base64,");
            }

            // ?json=true is the same request for a client that cannot set headers.
            try (Response response = client.request("/api/v0/qr/generate?json=true",
                    b -> b.post(jsonBody("{\"data\":\"embed me\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("image").asText()).startsWith("data:image/png;base64,");
            }
        });
    }

    @Test
    void qrGenerateRejectsWhatItCannotEncode() {
        JavalinTest.test(app, (server, client) -> {
            try (Response noData = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"size\":512}")))) {
                assertThat(noData.code()).isEqualTo(400);
                assertThat(json(noData).get("error").asText()).contains("'data' is required");
            }

            try (Response badFormat = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"data\":\"x\",\"format\":\"tiff\"}")))) {
                assertThat(badFormat.code()).isEqualTo(400);
                assertThat(json(badFormat).get("error").asText()).contains("unsupported format");
            }

            try (Response badColor = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"data\":\"x\",\"foreground\":\"salmon\"}")))) {
                assertThat(badColor.code()).isEqualTo(400);
                assertThat(json(badColor).get("error").asText()).contains("foreground must be a hex color");
            }

            String tooLong = "x".repeat(3000);
            try (Response tooMuch = client.request("/api/v0/qr/generate",
                    b -> b.post(jsonBody("{\"data\":\"" + tooLong + "\"}")))) {
                assertThat(tooMuch.code()).isEqualTo(400);
                assertThat(json(tooMuch).get("error").asText()).contains("too long");
            }
        });
    }

    @Test
    void qrReadAcceptsBase64AndARawImageBody() {
        JavalinTest.test(app, (server, client) -> {
            String dataUrl;
            try (Response generated = client.request("/api/v0/qr/generate?json=true",
                    b -> b.post(jsonBody("{\"data\":\"scan me\"}")))) {
                dataUrl = json(generated).get("image").asText();
            }

            // The data: URL exactly as it came back.
            try (Response read = client.request("/api/v0/qr/read",
                    b -> b.post(jsonBody(mapper.createObjectNode().put("image", dataUrl).toString())))) {
                assertThat(read.code()).isEqualTo(200);
                assertThat(json(read).get("text").asText()).isEqualTo("scan me");
            }

            // The same bytes posted raw.
            byte[] png = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
            try (Response read = client.request("/api/v0/qr/read",
                    b -> b.post(RequestBody.create(png, MediaType.parse("image/png"))))) {
                assertThat(read.code()).isEqualTo(200);
                assertThat(json(read).get("text").asText()).isEqualTo("scan me");
            }
        });
    }

    @Test
    void qrReadReportsAnImageWithoutACodeRatherThanFailing() {
        JavalinTest.test(app, (server, client) -> {
            // A PNG the server itself will render, of a code-free white square.
            byte[] blank = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAIAAAD/gAIDAAAAOklEQVR4nO3BMQEAAADCoPVPbQwf"
                + "oAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAvg0hAAABgSGF7wAAAABJRU5ErkJggg==");

            MultipartBody upload = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "blank.png", RequestBody.create(blank, MediaType.parse("image/png")))
                .build();

            try (Response read = client.request("/api/v0/qr/read", b -> b.post(upload))) {
                assertThat(read.code()).isEqualTo(200);
                JsonNode body = json(read);
                assertThat(body.get("found").asBoolean()).isFalse();
                assertThat(body.get("text").isNull()).isTrue();
            }
        });
    }

    @Test
    void qrReadRejectsWhatIsNotAnImage() {
        JavalinTest.test(app, (server, client) -> {
            try (Response nothing = client.request("/api/v0/qr/read", b -> b.post(jsonBody("{}")))) {
                assertThat(nothing.code()).isEqualTo(400);
                assertThat(json(nothing).get("error").asText()).contains("an image is required");
            }

            try (Response notBase64 = client.request("/api/v0/qr/read",
                    b -> b.post(jsonBody("{\"image\":\"%%%not base64%%%\"}")))) {
                assertThat(notBase64.code()).isEqualTo(400);
            }

            try (Response notAnImage = client.request("/api/v0/qr/read",
                    b -> b.post(RequestBody.create("hello".getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("image/png"))))) {
                assertThat(notAnImage.code()).isEqualTo(400);
                assertThat(json(notAnImage).get("error").asText()).contains("unsupported image format");
            }
        });
    }


    // region encoding

    @Test
    void base64EncodeAndDecodeRoundTrip() {
        JavalinTest.test(app, (server, client) -> {
            try (Response encoded = client.request("/api/v0/encoding/base64/encode",
                    b -> b.post(jsonBody("{\"data\":\"foobar\"}")))) {
                assertThat(encoded.code()).isEqualTo(200);
                JsonNode body = json(encoded);
                assertThat(body.get("data").asText()).isEqualTo("Zm9vYmFy");
                assertThat(body.get("bytes").asInt()).isEqualTo(6);
                assertThat(body.get("from").asText()).isEqualTo("utf8");
                assertThat(body.get("to").asText()).isEqualTo("base64");
            }

            try (Response decoded = client.request("/api/v0/encoding/base64/decode",
                    b -> b.post(jsonBody("{\"data\":\"Zm9vYmFy\"}")))) {
                assertThat(decoded.code()).isEqualTo(200);
                JsonNode body = json(decoded);
                assertThat(body.get("data").asText()).isEqualTo("foobar");
                assertThat(body.get("from").asText()).isEqualTo("base64");
                assertThat(body.get("to").asText()).isEqualTo("utf8");
            }
        });
    }

    @Test
    void hexEncodeReadsBinaryInputAndDecodeWritesIt() {
        JavalinTest.test(app, (server, client) -> {
            // `encoding` describes the input, so binary can be handed in as base64.
            try (Response encoded = client.request("/api/v0/encoding/hex/encode",
                    b -> b.post(jsonBody("{\"data\":\"Zm9vYmFy\",\"encoding\":\"base64\"}")))) {
                assertThat(encoded.code()).isEqualTo(200);
                assertThat(json(encoded).get("data").asText()).isEqualTo("666f6f626172");
            }

            // `outputEncoding` describes the result, so bytes that are not text still come back.
            try (Response decoded = client.request("/api/v0/encoding/hex/decode",
                    b -> b.post(jsonBody("{\"data\":\"0x66:6F:6F\",\"outputEncoding\":\"base64\"}")))) {
                assertThat(decoded.code()).isEqualTo(200);
                JsonNode body = json(decoded);
                assertThat(body.get("data").asText()).isEqualTo("Zm9v");
                assertThat(body.get("to").asText()).isEqualTo("base64");
            }
        });
    }

    @Test
    void urlEncodingHasBothVariants() {
        JavalinTest.test(app, (server, client) -> {
            try (Response rfc = client.request("/api/v0/encoding/url/encode",
                    b -> b.post(jsonBody("{\"data\":\"a b+c\"}")))) {
                assertThat(rfc.code()).isEqualTo(200);
                JsonNode body = json(rfc);
                assertThat(body.get("data").asText()).isEqualTo("a%20b%2Bc");
                assertThat(body.get("to").asText()).isEqualTo("url");
            }

            try (Response form = client.request("/api/v0/encoding/url/encode",
                    b -> b.post(jsonBody("{\"data\":\"a b+c\",\"form\":true}")))) {
                assertThat(form.code()).isEqualTo(200);
                JsonNode body = json(form);
                assertThat(body.get("data").asText()).isEqualTo("a+b%2Bc");
                assertThat(body.get("to").asText()).isEqualTo("url-form");
            }

            // The same text decoded the other way round means something different.
            try (Response rfc = client.request("/api/v0/encoding/url/decode",
                    b -> b.post(jsonBody("{\"data\":\"a+b\"}")))) {
                assertThat(json(rfc).get("data").asText()).isEqualTo("a+b");
            }
            try (Response form = client.request("/api/v0/encoding/url/decode",
                    b -> b.post(jsonBody("{\"data\":\"a+b\",\"form\":true}")))) {
                assertThat(json(form).get("data").asText()).isEqualTo("a b");
            }
        });
    }

    @Test
    void convertGoesBetweenAnyTwoFormats() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/encoding/convert",
                    b -> b.post(jsonBody("{\"data\":\"Zm9vYmFy\",\"from\":\"base64\",\"to\":\"base32\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("data").asText()).isEqualTo("MZXW6YTBOI======");
                assertThat(body.get("bytes").asInt()).isEqualTo(6);
                assertThat(body.get("from").asText()).isEqualTo("base64");
                assertThat(body.get("to").asText()).isEqualTo("base32");
            }

            // Format names are matched loosely, and echoed back canonically.
            try (Response response = client.request("/api/v0/encoding/convert",
                    b -> b.post(jsonBody("{\"data\":\"MZXW6YTBOI\",\"from\":\"BASE-32\",\"to\":\"base16\"}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("data").asText()).isEqualTo("666f6f626172");
                assertThat(body.get("to").asText()).isEqualTo("hex");
            }
        });
    }

    @Test
    void encodingRejectsWhatItCannotRead() {
        JavalinTest.test(app, (server, client) -> {
            try (Response noData = client.request("/api/v0/encoding/base64/encode",
                    b -> b.post(jsonBody("{}")))) {
                assertThat(noData.code()).isEqualTo(400);
                assertThat(json(noData).get("error").asText()).contains("'data' is required");
            }

            try (Response badBase64 = client.request("/api/v0/encoding/base64/decode",
                    b -> b.post(jsonBody("{\"data\":\"not base64!!\"}")))) {
                assertThat(badBase64.code()).isEqualTo(400);
                assertThat(json(badBase64).get("error").asText()).contains("not valid base64");
            }

            try (Response badFormat = client.request("/api/v0/encoding/convert",
                    b -> b.post(jsonBody("{\"data\":\"x\",\"from\":\"utf8\",\"to\":\"rot13\"}")))) {
                assertThat(badFormat.code()).isEqualTo(400);
                assertThat(json(badFormat).get("error").asText()).contains("unsupported to 'rot13'");
            }

            // Arbitrary bytes asked for as text are refused, not mangled into '?'.
            try (Response notText = client.request("/api/v0/encoding/hex/decode",
                    b -> b.post(jsonBody("{\"data\":\"80\"}")))) {
                assertThat(notText.code()).isEqualTo(400);
                assertThat(json(notText).get("error").asText()).contains("not valid UTF-8 text");
            }
        });
    }


    // region id + random

    @Test
    void uuidEndpointIssuesCanonicalV4ByDefault() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/id/uuid")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("type").asText()).isEqualTo("uuid");
                assertThat(body.get("version").asText()).isEqualTo("v4");
                assertThat(body.get("format").asText()).isEqualTo("canonical");
                assertThat(body.get("bits").asInt()).isEqualTo(122);
                assertThat(body.get("count").asInt()).isEqualTo(1);
                // One value still comes back as an array, so a client never branches on the count.
                assertThat(body.get("values").isArray()).isTrue();
                assertThat(body.get("values").get(0).asText())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            }
        });
    }

    @Test
    void uuidEndpointTakesCountVersionAndFormat() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/id/uuid?count=5&version=v7&format=compact")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("version").asText()).isEqualTo("v7");
                assertThat(body.get("format").asText()).isEqualTo("compact");
                assertThat(body.get("values")).hasSize(5);
                body.get("values").forEach(value ->
                    assertThat(value.asText()).matches("[0-9a-f]{12}7[0-9a-f]{19}"));
            }
        });
    }

    @Test
    void ulidEndpointIssuesSortedIds() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/id/ulid?count=4")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("type").asText()).isEqualTo("ulid");
                assertThat(body.get("bits").asInt()).isEqualTo(80);

                List<String> values = new ArrayList<>();
                body.get("values").forEach(value -> values.add(value.asText()));
                assertThat(values).hasSize(4).isSorted()
                    .allMatch(v -> v.matches("[0-9A-HJKMNP-TV-Z]{26}"));
            }

            try (Response response = client.get("/api/v0/id/ulid?format=uuid")) {
                assertThat(json(response).get("values").get(0).asText())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            }
        });
    }

    @Test
    void tokenEndpointTakesLengthAlphabetAndACustomSet() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/random/token?count=2&length=16&alphabet=base58")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("alphabet").asText()).isEqualTo("base58");
                assertThat(body.get("length").asInt()).isEqualTo(16);
                assertThat(body.get("values")).hasSize(2);
                body.get("values").forEach(value ->
                    assertThat(value.asText()).matches("[123456789A-HJ-NP-Za-km-z]{16}"));
            }

            try (Response response = client.get("/api/v0/random/token?length=8&chars=ATGC")) {
                JsonNode body = json(response);
                assertThat(body.get("alphabet").asText()).isEqualTo("custom");
                assertThat(body.get("bits").asInt()).isEqualTo(16);
                assertThat(body.get("values").get(0).asText()).matches("[ATGC]{8}");
            }
        });
    }

    @Test
    void randomBytesEndpointTakesLengthAndFormat() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/random/bytes?length=16")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("format").asText()).isEqualTo("hex");
                assertThat(body.get("bits").asInt()).isEqualTo(128);
                assertThat(body.get("values").get(0).asText()).matches("[0-9a-f]{32}");
            }

            try (Response response = client.get("/api/v0/random/bytes?length=32&format=base64")) {
                String value = json(response).get("values").get(0).asText();
                assertThat(Base64.getDecoder().decode(value)).hasSize(32);
            }
        });
    }

    @Test
    void batchMixesKindsInOneCall() {
        JavalinTest.test(app, (server, client) -> {
            String request = "{\"items\":["
                + "{\"type\":\"uuid\",\"count\":2,\"version\":\"v7\"},"
                + "{\"type\":\"ulid\",\"count\":3},"
                + "{\"type\":\"token\",\"length\":12,\"alphabet\":\"hex\"},"
                + "{\"type\":\"bytes\",\"length\":8,\"format\":\"base64url\"}]}";

            try (Response response = client.request("/api/v0/id/batch", b -> b.post(jsonBody(request)))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode items = json(response).get("items");
                assertThat(items).hasSize(4);
                assertThat(items.get(0).get("type").asText()).isEqualTo("uuid");
                assertThat(items.get(0).get("values")).hasSize(2);
                assertThat(items.get(1).get("values")).hasSize(3);
                assertThat(items.get(2).get("values").get(0).asText()).matches("[0-9a-f]{12}");
                assertThat(items.get(3).get("format").asText()).isEqualTo("base64url");
            }
        });
    }

    @Test
    void batchAlsoTakesASingleItemUnwrapped() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/id/batch",
                    b -> b.post(jsonBody("{\"type\":\"uuid\",\"count\":3}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode items = json(response).get("items");
                assertThat(items).hasSize(1);
                assertThat(items.get(0).get("values")).hasSize(3);
            }
        });
    }

    @Test
    void idEndpointsRejectWhatTheyCannotGenerate() {
        JavalinTest.test(app, (server, client) -> {
            try (Response notANumber = client.get("/api/v0/id/uuid?count=lots")) {
                assertThat(notANumber.code()).isEqualTo(400);
                assertThat(json(notANumber).get("error").asText()).contains("count must be a whole number");
            }

            try (Response outOfRange = client.get("/api/v0/id/uuid?count=0")) {
                assertThat(outOfRange.code()).isEqualTo(400);
                assertThat(json(outOfRange).get("error").asText()).contains("count must be between 1 and 1000");
            }

            try (Response badAlphabet = client.get("/api/v0/random/token?alphabet=emoji")) {
                assertThat(badAlphabet.code()).isEqualTo(400);
                assertThat(json(badAlphabet).get("error").asText()).contains("unsupported alphabet 'emoji'");
            }

            try (Response asText = client.get("/api/v0/random/bytes?format=utf8")) {
                assertThat(asText.code()).isEqualTo(400);
                assertThat(json(asText).get("error").asText()).contains("random bytes are not text");
            }

            try (Response noType = client.request("/api/v0/id/batch",
                    b -> b.post(jsonBody("{\"count\":3}")))) {
                assertThat(noType.code()).isEqualTo(400);
            }

            try (Response tooMany = client.request("/api/v0/id/batch",
                    b -> b.post(jsonBody("{\"items\":[{\"type\":\"uuid\",\"count\":600},"
                        + "{\"type\":\"ulid\",\"count\":600}]}")))) {
                assertThat(tooMany.code()).isEqualTo(400);
                assertThat(json(tooMany).get("error").asText()).contains("at most 1000 values in total");
            }
        });
    }


    // region json

    @Test
    void jsonValidateAnswersWhereADocumentStopsBeingJson() {
        JavalinTest.test(app, (server, client) -> {
            try (Response ok = client.request("/api/v0/json/validate",
                    b -> b.post(jsonBody("{\"a\": [1, 2, 3]}")))) {
                assertThat(ok.code()).isEqualTo(200);
                JsonNode body = json(ok);
                assertThat(body.get("valid").asBoolean()).isTrue();
                assertThat(body.get("bytes").asInt()).isEqualTo(16);
            }

            // A broken document is a 200 with the reason, not an error status:
            // reporting where the problem is, is the whole job.
            try (Response broken = client.request("/api/v0/json/validate",
                    b -> b.post(jsonBody("{\n  \"a\": 1,\n  \"b\":\n}")))) {
                assertThat(broken.code()).isEqualTo(200);
                JsonNode body = json(broken);
                assertThat(body.get("valid").asBoolean()).isFalse();
                assertThat(body.get("line").asInt()).isEqualTo(4);
                assertThat(body.get("error").asText()).isNotBlank();
            }

            try (Response duplicate = client.request("/api/v0/json/validate",
                    b -> b.post(jsonBody("{\"a\":1,\"a\":2}")))) {
                JsonNode body = json(duplicate);
                assertThat(body.get("valid").asBoolean()).isFalse();
                assertThat(body.get("error").asText().toLowerCase()).contains("duplicate");
            }
        });
    }

    @Test
    void jsonFormatAnswersWithTheDocumentItself() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/json/format",
                    b -> b.post(jsonBody("{\"b\":1,\"a\":[1,2]}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.header("Content-Type")).contains("application/json");
                // Not wrapped in a field — a caller should not have to unescape
                // a JSON document out of a JSON string.
                assertThat(response.body().string())
                    .isEqualTo("{\n  \"b\": 1,\n  \"a\": [\n    1,\n    2\n  ]\n}");
            }

            try (Response response = client.request("/api/v0/json/format?indent=4&sort=true",
                    b -> b.post(jsonBody("{\"b\":1,\"a\":2}")))) {
                assertThat(response.body().string()).isEqualTo("{\n    \"a\": 2,\n    \"b\": 1\n}");
            }

            try (Response response = client.request("/api/v0/json/format?indent=tab",
                    b -> b.post(jsonBody("{\"a\":1}")))) {
                assertThat(response.body().string()).isEqualTo("{\n\t\"a\": 1\n}");
            }
        });
    }

    @Test
    void jsonMinifyStripsEveryAvoidableByte() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/json/minify",
                    b -> b.post(jsonBody("{\n  \"a\" : 1,\n  \"b\" : [ 1, 2 ]\n}")))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).isEqualTo("{\"a\":1,\"b\":[1,2]}");
            }
        });
    }

    @Test
    void jsonQueryTakesAPointerOrAPath() {
        JavalinTest.test(app, (server, client) -> {
            String document = "{\"store\":{\"book\":[{\"title\":\"a\",\"price\":5},"
                + "{\"title\":\"b\",\"price\":15}]}}";

            try (Response pointer = client.request("/api/v0/json/query?pointer=/store/book/1/title",
                    b -> b.post(jsonBody(document)))) {
                assertThat(pointer.code()).isEqualTo(200);
                JsonNode body = json(pointer);
                assertThat(body.get("syntax").asText()).isEqualTo("pointer");
                assertThat(body.get("count").asInt()).isEqualTo(1);
                assertThat(body.get("matches").get(0).asText()).isEqualTo("b");
            }

            try (Response path = client.request("/api/v0/json/query?path=$..book%5B%3F(@.price%20%3C%2010)%5D.title",
                    b -> b.post(jsonBody(document)))) {
                assertThat(path.code()).isEqualTo(200);
                JsonNode body = json(path);
                assertThat(body.get("syntax").asText()).isEqualTo("jsonpath");
                assertThat(body.get("count").asInt()).isEqualTo(1);
                assertThat(body.get("matches").get(0).asText()).isEqualTo("a");
                assertThat(body.get("paths").get(0).asText()).contains("book");
            }

            // Nothing at that path is an answer, not a 404.
            try (Response none = client.request("/api/v0/json/query?pointer=/store/dvd",
                    b -> b.post(jsonBody(document)))) {
                assertThat(none.code()).isEqualTo(200);
                assertThat(json(none).get("count").asInt()).isZero();
            }
        });
    }

    @Test
    void jsonDiffReturnsAPatchThatDescribesTheChange() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.request("/api/v0/json/diff",
                    b -> b.post(jsonBody("{\"from\":{\"a\":1,\"gone\":true},"
                        + "\"to\":{\"a\":2,\"added\":\"x\"}}")))) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("equal").asBoolean()).isFalse();
                assertThat(body.get("operations").asInt()).isEqualTo(3);

                List<String> operations = new ArrayList<>();
                body.get("patch").forEach(op ->
                    operations.add(op.get("op").asText() + " " + op.get("path").asText()));
                assertThat(operations).containsExactlyInAnyOrder(
                    "remove /gone", "replace /a", "add /added");
            }

            try (Response same = client.request("/api/v0/json/diff",
                    b -> b.post(jsonBody("{\"from\":{\"a\":1},\"to\":{\"a\":1.0}}")))) {
                // RFC 6902 compares numbers by value: 1 and 1.0 are one number.
                JsonNode body = json(same);
                assertThat(body.get("equal").asBoolean()).isTrue();
                assertThat(body.get("patch")).isEmpty();
            }
        });
    }

    @Test
    void jsonEndpointsRejectWhatTheyCannotRead() {
        JavalinTest.test(app, (server, client) -> {
            try (Response broken = client.request("/api/v0/json/format",
                    b -> b.post(jsonBody("{\"a\":}")))) {
                assertThat(broken.code()).isEqualTo(400);
                assertThat(json(broken).get("error").asText()).contains("invalid JSON at line 1");
            }

            try (Response badIndent = client.request("/api/v0/json/format?indent=99",
                    b -> b.post(jsonBody("{\"a\":1}")))) {
                assertThat(badIndent.code()).isEqualTo(400);
                assertThat(json(badIndent).get("error").asText()).contains("between 1 and 16");
            }

            try (Response noExpression = client.request("/api/v0/json/query",
                    b -> b.post(jsonBody("{\"a\":1}")))) {
                assertThat(noExpression.code()).isEqualTo(400);
                assertThat(json(noExpression).get("error").asText()).contains("expression is required");
            }

            try (Response badPointer = client.request("/api/v0/json/query?pointer=a/b",
                    b -> b.post(jsonBody("{\"a\":1}")))) {
                assertThat(badPointer.code()).isEqualTo(400);
                assertThat(json(badPointer).get("error").asText()).contains("start with '/'");
            }

            try (Response halfADiff = client.request("/api/v0/json/diff",
                    b -> b.post(jsonBody("{\"from\":{\"a\":1}}")))) {
                assertThat(halfADiff.code()).isEqualTo(400);
                assertThat(json(halfADiff).get("error").asText()).contains("both 'from' and 'to' are required");
            }
        });
    }

    // endregion

    // region repository links

    @Test
    void linkRepositoryLifecycle() {
        JavalinTest.test(app, (server, client) -> {
            // create repository
            String repositoryId;
            String token;
            try (Response created = client.request("/api/v0/repository/links/",
                    b -> b.post(jsonBody("{\"name\":\"my repo\"}")))) {
                assertThat(created.code()).isEqualTo(201);
                JsonNode body = json(created);
                repositoryId = body.get("repositoryId").asText();
                token = body.get("token").asText();
                assertThat(body.get("name").asText()).isEqualTo("my repo");
            }

            String repoPath = "/api/v0/repository/links/" + repositoryId;

            // wrong token → 404 (does not reveal repository existence)
            try (Response wrongToken = client.get(repoPath + "?token=wrong")) {
                assertThat(wrongToken.code()).isEqualTo(404);
            }

            // add a link
            String linkId;
            try (Response linkCreated = client.request(repoPath + "/links?token=" + token,
                    b -> b.post(jsonBody("{\"url\":\"https://example.com\",\"name\":\"ex\"}")))) {
                assertThat(linkCreated.code()).isEqualTo(201);
                linkId = json(linkCreated).get("uuid").asText();
            }

            // repository listing contains the link
            try (Response fetched = client.get(repoPath + "?token=" + token)) {
                assertThat(fetched.code()).isEqualTo(200);
                JsonNode body = json(fetched);
                assertThat(body.get("links")).hasSize(1);
                assertThat(body.get("links").get(0).get("url").asText()).isEqualTo("https://example.com");
            }

            // update the link
            try (Response updated = client.request(repoPath + "/links/" + linkId + "?token=" + token,
                    b -> b.put(jsonBody("{\"url\":\"https://updated.example.com\"}")))) {
                assertThat(updated.code()).isEqualTo(204);
            }

            // update with blank url → 400
            try (Response badUpdate = client.request(repoPath + "/links/" + linkId + "?token=" + token,
                    b -> b.put(jsonBody("{\"url\":\"  \"}")))) {
                assertThat(badUpdate.code()).isEqualTo(400);
            }

            // delete the link
            try (Response linkDeleted = client.delete(repoPath + "/links/" + linkId + "?token=" + token)) {
                assertThat(linkDeleted.code()).isEqualTo(204);
            }

            // delete the repository
            try (Response repoDeleted = client.delete(repoPath + "?token=" + token)) {
                assertThat(repoDeleted.code()).isEqualTo(204);
            }
            try (Response gone = client.get(repoPath + "?token=" + token)) {
                assertThat(gone.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void linkRepositoryRequestsWithoutTokenAreRejected() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get(
                    "/api/v0/repository/links/00000000-0000-0000-0000-000000000000")) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    // endregion

    // region uploaded files

    @Test
    void uploadedFilesLifecycle() {
        JavalinTest.test(app, (server, client) -> {
            byte[] content = "uploaded file content".getBytes(StandardCharsets.UTF_8);
            MultipartBody upload = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "test.txt",
                    RequestBody.create(content, MediaType.parse("application/octet-stream")))
                .addFormDataPart("name", "custom-name.txt")
                .build();

            // upload
            String uuid;
            String token;
            try (Response created = client.request("/api/v0/uploaded-files", b -> b.post(upload))) {
                assertThat(created.code()).isEqualTo(201);
                JsonNode body = json(created);
                uuid = body.get("uuid").asText();
                token = body.get("token").asText();
                assertThat(body.get("name").asText()).isEqualTo("custom-name.txt");
            }

            String filePath = "/api/v0/uploaded-files/" + uuid;

            // metadata requires the token
            try (Response noToken = client.get(filePath)) {
                assertThat(noToken.code()).isEqualTo(403);
            }
            try (Response metadata = client.get(filePath + "?token=" + token)) {
                assertThat(metadata.code()).isEqualTo(200);
                assertThat(json(metadata).get("name").asText()).isEqualTo("custom-name.txt");
            }

            // download returns the original bytes
            try (Response download = client.get(filePath + "/download?token=" + token)) {
                assertThat(download.code()).isEqualTo(200);
                assertThat(download.body().bytes()).isEqualTo(content);
            }

            // delete requires the token
            try (Response forbidden = client.delete(filePath + "?token=wrong")) {
                assertThat(forbidden.code()).isEqualTo(403);
            }
            try (Response deleted = client.delete(filePath + "?token=" + token)) {
                assertThat(deleted.code()).isEqualTo(204);
            }
            try (Response gone = client.get(filePath + "?token=" + token)) {
                assertThat(gone.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void uploadedFilesListingIsForbidden() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/uploaded-files")) {
                assertThat(response.code()).isEqualTo(403);
            }
        });
    }

    @Test
    void uploadedFilesMaxSizeEndpoint() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/uploaded-files/max-size")) {
                assertThat(response.code()).isEqualTo(200);
                JsonNode body = json(response);
                assertThat(body.get("megabytes").asInt()).isEqualTo(20);
                assertThat(body.get("bytes").asLong()).isEqualTo(20L * 1024 * 1024);
            }
        });
    }

    @Test
    void uploadedFilesRejectInvalidUuidAndMissingFile() {
        JavalinTest.test(app, (server, client) -> {
            try (Response badUuid = client.get("/api/v0/uploaded-files/not-a-uuid?token=x")) {
                assertThat(badUuid.code()).isEqualTo(400);
            }
            // POST without multipart file part
            try (Response noFile = client.request("/api/v0/uploaded-files",
                    b -> b.post(new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("name", "x").build()))) {
                assertThat(noFile.code()).isEqualTo(400);
            }
        });
    }

    // endregion

    // region youtube

    @Test
    void youtubeInfoRequiresUrlParameter() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/youtube/info")) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    void youtubeInfoRejectsNonYoutubeDomain() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/api/v0/youtube/info?url=https://evil.com/watch")) {
                assertThat(response.code()).isEqualTo(400);
                assertThat(response.body().string()).contains("not an allowed YouTube domain");
            }
        });
    }

    @Test
    void youtubeInfoReturnsDataForAllowedDomain() throws Exception {
        when(youtube.getVideoInfo("https://www.youtube.com/watch?v=abc"))
            .thenReturn("{\"title\":\"Mock Video\"}");

        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get(
                    "/api/v0/youtube/info?url=https://www.youtube.com/watch%3Fv%3Dabc")) {
                // query param is url-encoded so the whole value survives
                assertThat(response.code()).isEqualTo(200);
                assertThat(json(response).get("title").asText()).isEqualTo("Mock Video");
            }
        });
    }

    // endregion
}
