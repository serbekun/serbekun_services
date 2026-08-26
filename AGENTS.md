# serbekun_services

## Instructions for AI agents

When the user states any preferences about coding style, architecture, conventions, or project direction, **append them to this file** so the information persists across chat sessions. If something is unclear or contradicts existing content, ask the user for clarification before changing code. This file is the canonical reference for project conventions — keep it up to date.

## Quick start

```sh
./gradlew build                 # compile + test
./gradlew shadowJar             # fat JAR → build/libs/serbekun_services-<version>-all.jar
java -jar build/libs/serbekun_services-<version>-all.jar  # server on :8080
./gradlew test                  # JUnit 5
./gradlew compileJava           # compile only
```

> The Gradle wrapper (`./gradlew`, Gradle 8.6) is the canonical build tool. System `gradle` from `/opt/gradle/bin/gradle` also works.

No lint, typecheck, or codegen tasks exist. No CI/CD.

## Project structure

| Layer          | Package                              | Role |
|----------------|--------------------------------------|------|
| Bootstrap      | `com.serbekun.Main`                  | Entry point. Manual dependency wiring via inner `ServerContext` / `Repositories` / `Services` / `Resources` / `Handlers` classes. No DI framework. |
| Config         | `com.serbekun.ss.config`             | `Config` (port), `Paths` — hardcoded static configuration. |
| Domain         | `com.serbekun.ss.domain.*`           | Domain entities (`Link`, `LinkRepository`, `ShortUrl`, `UploadedFile`) and HTTP DTOs (`domain/dto/http/*`). No framework dependencies. |
| Repository     | `com.serbekun.ss.repo.*`             | Repository **interfaces** + JSON-file implementations (Jackson). All implement `AutoSavable`. |
| Service        | `com.serbekun.ss.service.*`          | Business logic and orchestration only (auth, cipher, hash, link repositories, short urls, resources, tokens, autosave, youtube, uploaded files cleanup). |
| HTTP (thin)    | `com.serbekun.ss.http.handles.*`     | Javalin route registration, DTOs mapping, validation, and service calls. **No business logic**. |
| Infrastructure | `com.serbekun.ss.infrastructure.*`   | Low-level technical components (FS initialization, autosave scheduler). |
| Resources      | `com.serbekun.ss.resources.*`        | Load and cache static files from classpath (`src/main/resources/`). |

## Architecture notes

- **Clean Layered Architecture** — full refactoring completed in 2026. The previous anti-pattern `service/http/handles/` (business logic mixed with HTTP) has been completely removed.
- **Domain** is isolated (`domain/models/`). All domain models are plain Java classes with Jackson annotations only where needed for persistence.
- **Repository interfaces** exist for all data access (`LinkRepositoryRepo`, `ShortUrlRepo`, `EndpointsAccessTokensRepo`, `UploadedFilesRepo`). Each has a `*FileRepo` JSON-file implementation and a `*ReadInterface` read-only view.
- **Services** contain all business rules and orchestration. They depend only on repository interfaces and other services. `Youtube` is a utility service (not wired into the DI context).
- **HTTP layer is thin** — only handles routing, DTO mapping, basic validation, and delegates to services.
- **No DI framework** — manual constructor injection via inner context classes in `Main.java`.
- **File-based storage** — all data persists as JSON under `repository/` (auto-created on startup). The file `repository/endpoint_access_tokens.json` holds auth tokens. Uploaded file content is stored under `repository/uploaded_files_raw/`.
- **Auth model** — endpoints registered via `EndpointRegistry`. Javalin `before` handler supports `Authorization` header and `?token=` / `?Authorization=Bearer ...` query params. `requiresAuth` is currently `false` for all endpoints.
- **Autosave** — `ScheduledExecutorService` calls `save()` on all `AutoSavable` repositories every **20 seconds**.
- **File cleanup** — `UploadedFilesCleanupService` periodically removes expired uploaded files (based on `expired_time` field). Runs on a separate scheduled executor.
- **Server port** — loaded from `repository/config.json` at startup (defaults to `8080`).
- **Java 21** with `-parameters` compiler flag (method parameter names preserved for Jackson).

## Current state after refactoring

The project now strictly follows **Clean Layered Architecture** with clear separation of concerns:

- `domain/models` — core business objects
- `repository` — data access (interfaces + impl)
- `service` — business logic
- `http/handles` — thin HTTP adapters
- `infrastructure` + `config` + `resources` — supporting technical layers

All previous violations (business logic in HTTP-related packages) have been eliminated.

For standalone frontend tools that do not need backend behavior, add only static files under `src/main/resources` and rely on existing static routing instead of changing server code.

## URLs

The project is a Javalin 6 server with token-based auth, JSON-file persistence, and static resource serving.

## Tests

JUnit 5 + AssertJ + Mockito, run with `./gradlew test`. Coverage (as of 2026-07-19):

- **Unit tests** per layer: services (`cipher` — AES, RSA, hybrid, `shorturl`, `linksrepo`, `uploadedfiles`, `auth`, `resource`), in-memory repos + JSON `*FileRepo` persistence roundtrips (`@TempDir`), `Config` loading/defaults/legacy-field migration, domain model validation + Jackson roundtrips, `ResourceCache`/`ResourcesBasePath`.
- **HTTP integration tests** at `src/test/java/com/serbekun/ss/http/ServerHttpIntegrationTest.java` — full route tree via `RouteInitializer` + `javalin-testtools`, real services over in-memory repos; only the `Youtube` (yt-dlp) wrapper is mocked. Covers index/static, version, cipher, short-url, repository-links, uploaded-files (multipart), and youtube endpoints.
- `YoutubeTest` contains real yt-dlp integration tests that need `yt-dlp`, Deno, and network access — expect failures without them.

Gotcha: don't use Mockito `verify(mock, timeout(...))` on `synchronized` methods (e.g. `UploadedFilesService.deleteExpiredFiles`) — the verifying thread holds the mock's monitor and deadlocks the thread under test. Use `CountDownLatch` answers instead (see `UploadedFilesCleanupServiceTest`).

## API endpoints (v0)

All registered in `http/handles/*Routes` classes:

- `GET /` — index page
- `GET /static/v0/images/{name}` — static images
- `GET /static/v0/json` / `GET /static/v0/json/{name}` — static JSON
- `GET /static/v0/html/{name}` — static HTML pages
- `GET /api/v0/version` — server version info
- `GET /api/v0/cipher/aes` — cipher info
- `POST /api/v0/cipher/aes/encrypt` — AES encrypt
- `POST /api/v0/cipher/aes/decrypt` — AES decrypt
- `GET /api/v0/cipher/rsa/keypair` — generate an RSA-2048 key pair, returns `{"publicKey", "privateKey"}` (Base64 X.509 / PKCS#8, never stored)
- `POST /api/v0/cipher/rsa/encrypt` — RSA-OAEP-SHA256 encrypt (JSON body `{"data", "publicKey"}`), max 190 bytes of payload
- `POST /api/v0/cipher/rsa/decrypt` — RSA decrypt (JSON body `{"data", "privateKey"}`)
- `POST /api/v0/cipher/rsa/sign` — SHA256withRSA sign (JSON body `{"data", "privateKey"}`), returns `{"signature"}`
- `POST /api/v0/cipher/rsa/verify` — verify a signature (JSON body `{"data", "signature", "publicKey"}`), returns `{"valid"}`; a mismatch is a 200 with `valid: false`, not an error
- `POST /api/v0/cipher/hybrid/encrypt` — hybrid encrypt of any size (JSON body `{"data", "publicKey"}`), returns `{"data", "encryptedKey"}`
- `POST /api/v0/cipher/hybrid/decrypt` — hybrid decrypt (JSON body `{"data", "encryptedKey", "privateKey"}`)
- `POST /api/v0/hash` — hash a payload (JSON body `{"algorithm", "data", "encoding"?, "key"?}`), returns `{"algorithm", "hash", "bytes"}`
- `POST /api/v0/hash/file` — hash an uploaded file (multipart `file`, plus optional `algorithm`, `key`, `encoding` fields), returns `{"algorithm", "hash", "bytes", "name"}`; streamed, nothing stored
- `POST /api/v0/hash/verify` — integrity check (JSON body `{"algorithm", "data", "hash", "encoding"?, "key"?}`), returns `{"valid", "algorithm", "expected", "actual"}`; a mismatch is a 200 with `valid: false`, not an error
- `POST /api/v0/repository/links/` — create a link repository (JSON body `{"name"?}`), returns `{"repositoryId", "token", "name", "createdAt"}` (the `token` is shown only here)
- `GET /api/v0/repository/links/{repositoryId}?token=...` — get a repository with all its links, 404 if not found or token is invalid
- `DELETE /api/v0/repository/links/{repositoryId}?token=...` — delete a repository, 204 on success, 404 if not found or token is invalid
- `POST /api/v0/repository/links/{repositoryId}/links?token=...` — add a link (JSON body `{"url", "name"?, "description"?}`), returns `{"uuid", "url", "name", "description"}`
- `PUT /api/v0/repository/links/{repositoryId}/links/{uuid}?token=...` — update a link (JSON body `{"url", "name"?, "description"?}`), 204 on success, 400 if `url` is blank, 404 if not found or token is invalid
- `DELETE /api/v0/repository/links/{repositoryId}/links/{uuid}?token=...` — delete a link, 204 on success, 404 if not found or token is invalid
- `GET /api/v0/youtube/info?url=...` — get video metadata as JSON
- `GET /api/v0/youtube/download?url=...` — download video and return MP4 bytes
- `POST /api/v0/short-url` — create a short url (JSON body `{"url", "name"?, "description"?}`), returns `{"id", "token"}`
- `GET /api/v0/short-url/{id}` — redirect (302) to the target url, 404 if unknown
- `DELETE /api/v0/short-url/{id}` — delete a short url; requires the delete `token` (`?token=` query param or JSON body), 403 on mismatch, 404 if unknown
- `GET /api/v0/uploaded-files` — list all uploaded files metadata
- `GET /api/v0/uploaded-files/max-size` — get maximum upload file size as `{"megabytes", "bytes"}`
- `GET /api/v0/uploaded-files/{uuid}` — get metadata for a single uploaded file
- `GET /api/v0/uploaded-files/{uuid}/download` — download uploaded file content (returns 404 if expired or not found)
- `POST /api/v0/uploaded-files` — upload a file (multipart form data), returns metadata with `uuid` and `token`
- `DELETE /api/v0/uploaded-files/{uuid}` — delete uploaded file; requires the delete `token` (`?token=` query param), 403 on mismatch, 404 if unknown

## Cipher functionality

All crypto lives in `service/cipher/` and is stateless — no key is ever written to disk, so whatever
a call needs must travel in the request. Every key and payload crosses the wire as Base64.

- `AesService` — AES-256-GCM, random 12-byte IV per call prepended to the cipher text.
- `RsaService` — RSA-2048; `RSA/ECB/OAEPPadding` with SHA-256 for encryption, `SHA256withRSA` for
  signatures. Keys are Base64 DER (X.509 public / PKCS#8 private). Unusable Base64 or an unparsable
  key raises `IllegalArgumentException`, which the HTTP layer maps to `400`; everything else is a
  `RuntimeException` and becomes `500`. `verify` returns `false` for a non-matching signature
  instead of throwing — a bad signature is an answer, not a failure.
- `HybridService` — the answer to RSA's ~190 byte ceiling: a fresh single-use AES key encrypts the
  payload, RSA-OAEP wraps only that key, and both halves come back together.
- `CipherService` — the facade the HTTP layer talks to; it only delegates.

Frontend: `html/cipher_aes.html` (AES) and `html/cipher_rsa.html` (RSA key pairs, encrypt/decrypt,
sign/verify, and hybrid encryption with file support). The hybrid tabs exchange an *envelope* —
`{"encryptedKey", "data", "name"?, "type"?}` — which is a frontend convention, not an API shape;
the file name in it is metadata in the clear, only the payload is encrypted.

## Hash functionality

`service/hash/` — stateless digests and integrity checks, no storage.

- `HashAlgorithm` — the supported set (`sha256`, `sha512`, `blake3`, `hmac-sha256`) plus loose parsing
  of caller spelling (`SHA-256`, `sha_256`) and one canonical `wireName()` echoed in every response.
- `HashService` — one-shot and streaming variants of each algorithm. Files stream through a 64 KB
  buffer so a large upload never has to fit in memory. `verify` compares with
  `MessageDigest.isEqual`, i.e. constant time, so repeated guesses cannot be timed to recover an HMAC.
  A non-matching digest is a `false`, not an exception.
- **BLAKE3** is the one algorithm the JDK does not ship. It comes from
  `io.github.rctcwyvrn:blake3` (pure Java, ~10 KB, no transitive deps). That implementation takes
  whole arrays only, so the streaming path trims a short final read before feeding it —
  `HashServiceTest` pins all 34 official BLAKE3 test vectors, one-shot **and** streamed, precisely
  because that trimming is where such a bug would hide.

On the wire, `data` is UTF-8 text by default so `{"algorithm":"sha256","data":"hello"}` works as
written; `"encoding": "base64"` or `"hex"` switches how both `data` and `key` are read. Digests are
always lowercase hex. On `/hash/file` the file is raw bytes, so `encoding` there describes the key only.

Frontend: `html/hash.html` — algorithm picker shared across text / file / verify tabs, drag-and-drop
file input, and a failed check showing expected against actual.

## YouTube functionality

- **Requires**: `yt-dlp` command-line tool installed and in PATH
- **JavaScript Runtime**: Requires Deno for signature solving on modern YouTube videos
  - Install: `curl -fsSL https://deno.land/x/install/install.sh | sh`
  - Default location: `~/.deno/bin/deno`
- **Cookies**: Uses `repository/www.youtube.com_cookies.txt` for YouTube authentication
- **Current limitations**:
  - Age-restricted videos require authenticated cookies from a logged-in browser
  - Playlist URLs (with `&list=` parameters) are skipped (`--no-playlist` flag)
  
### Implementation notes (2026-06-12)
- Added 120-second timeout to prevent yt-dlp processes from hanging indefinitely
- Uses `--no-playlist` flag to ignore playlist parameters in URLs
- Uses Deno JavaScript runtime for signature solving (`--js-runtimes deno`)
- Dynamically adds Deno to PATH when starting yt-dlp process
- Process timeout with `waitFor(timeout, TimeUnit.SECONDS)` and `destroyForcibly()` on timeout
