package com.serbekun.ss.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.BuildInfo;
import com.serbekun.ss.config.Config;
import com.serbekun.ss.http.handles.RouteInitializer;
import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo;
import com.serbekun.ss.repo.shorturl.ShortUrlRepo;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesRepo;
import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.EndpointRegistry;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.shorturl.ShortUrlService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.Youtube;
import com.serbekun.ss.service.youtube.YoutubeDomains;
import com.serbekun.ss.service.youtube.YoutubeService;

import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests: the full route tree from {@link RouteInitializer} wired
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
        Config config = new Config(0, 20, null, 30, "yt-dlp", "deno");

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

        app = Javalin.create(cfg -> {
            cfg.http.maxRequestSize = config.getUploadFileMaxSizeBytes() + 1024L * 1024L;
            cfg.jetty.multipartConfig.maxFileSize(config.getUploadFileMaxSizeMb(), SizeUnit.MB);
        });
        app.before(ctx -> ctx.res().setCharacterEncoding("UTF-8"));

        new RouteInitializer().initHandles(
            app, resourcesService, authService, endpointRegistry,
            resourcesService, linkService, new CipherService(),
            youtubeService, uploadedService, shortUrlService, config);
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
