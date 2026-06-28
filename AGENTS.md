# serbekun_services

## Instructions for AI agents

When the user states any preferences about coding style, architecture, conventions, or project direction, **append them to this file** so the information persists across chat sessions. If something is unclear or contradicts existing content, ask the user for clarification before changing code. This file is the canonical reference for project conventions — keep it up to date.

## Quick start

```sh
gradle build                 # compile + test
gradle shadowJar             # fat JAR → build/libs/serbekun_services-alfa-2026-06-23-all.jar
java -jar build/libs/serbekun_services-alfa-2026-06-23-all.jar  # server on :8080
gradle test                  # JUnit 5
gradle compileJava           # compile only
```

> **Important**: `gradlew` wrapper is broken (missing `gradle/wrapper/`). Always use system `gradle` from `/opt/gradle/bin/gradle`.

No lint, typecheck, or codegen tasks exist. No CI/CD.

## Project structure

| Layer          | Package                              | Role |
|----------------|--------------------------------------|------|
| Bootstrap      | `com.serbekun.Main`                  | Entry point. Manual dependency wiring via inner `ServerContext` / `Repositories` / `Services` / `Resources` / `Handlers` classes. No DI framework. |
| Config         | `com.serbekun.ss.config`             | `Config` (port), `Paths` — hardcoded static configuration. |
| Domain         | `com.serbekun.ss.domain.*`           | Domain entities (`Link`, `Links`, `ShortUrl`, `UploadedFile`) and HTTP DTOs (`domain/dto/http/*`). No framework dependencies. |
| Repository     | `com.serbekun.ss.repo.*`             | Repository **interfaces** + JSON-file implementations (Jackson). All implement `AutoSavable`. |
| Service        | `com.serbekun.ss.service.*`          | Business logic and orchestration only (auth, cipher, links, resources, tokens, autosave, youtube, uploaded files cleanup). |
| HTTP (thin)    | `com.serbekun.ss.http.handles.*`     | Javalin route registration, DTOs mapping, validation, and service calls. **No business logic**. |
| Infrastructure | `com.serbekun.ss.infrastructure.*`   | Low-level technical components (FS initialization, autosave scheduler). |
| Resources      | `com.serbekun.ss.resources.*`        | Load and cache static files from classpath (`src/main/resources/`). |

## Architecture notes

- **Clean Layered Architecture** — full refactoring completed in 2026. The previous anti-pattern `service/http/handles/` (business logic mixed with HTTP) has been completely removed.
- **Domain** is isolated (`domain/models/`). All domain models are plain Java classes with Jackson annotations only where needed for persistence.
- **Repository interfaces** exist for all data access (`LinksRepository`, `LocalTokensRepository`, `EndpointAccessTokensRepository`, `UploadedFilesRepo`). Implementations are in `*Impl` classes.
- **Services** contain all business rules and orchestration. They depend only on repository interfaces and other services. `Youtube` is a utility service (not wired into the DI context).
- **HTTP layer is thin** — only handles routing, DTO mapping, basic validation, and delegates to services.
- **No DI framework** — manual constructor injection via inner context classes in `Main.java`.
- **File-based storage** — all data persists as JSON under `repository/` (auto-created on startup). The file `repository/endpoint_access_tokens.json` holds auth tokens. Uploaded file content is stored under `repository/uploaded_files_raw/`.
- **Auth model** — endpoints registered via `EndpointRegistry`. Javalin `before` handler supports `Authorization` header and `?token=` / `?Authorization=Bearer ...` query params. `requiresAuth` is currently `false` for all endpoints.
- **Autosave** — `ScheduledExecutorService` calls `save()` on all `AutoSavable` repositories every **20 seconds**.
- **File cleanup** — `UploadedFilesCleanupService` periodically removes expired uploaded files (based on `expired_time` field). Runs on a separate scheduled executor.
- **Server port** — loaded from `repository/config.json` at startup (defaults to `8080`).
- **Java 21** with `-parameters` compiler flag (method parameter names preserved for Jackson).
- **Dead dependencies in `build.gradle`**: H2, Flyway, Hibernate Validator, Guava, Javalin SSL plugin, Jakarta Validation API are declared but unused.

## Current state after refactoring

The project now strictly follows **Clean Layered Architecture** with clear separation of concerns:

- `domain/models` — core business objects
- `repository` — data access (interfaces + impl)
- `service` — business logic
- `http/handles` — thin HTTP adapters
- `infrastructure` + `config` + `resources` — supporting technical layers

All previous violations (business logic in HTTP-related packages) have been eliminated.

## URLs

The project is a Javalin 6 server with token-based auth, JSON-file persistence, and static resource serving.

## Tests

Single unit test at `src/test/java/com/serbekun/ss/service/resource/ResourceServiceTest.java` — uses Mockito + AssertJ. No integration tests. Run with `gradle test`.

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
- `GET /api/v0/catalogs/links` — list links
- `POST /api/v0/catalogs/links` — create link
- `PUT /api/v0/catalogs/links/{uuid}` — update link
- `DELETE /api/v0/catalogs/links/{uuid}` — delete link
- `GET /api/v0/youtube/info?url=...` — get video metadata as JSON
- `GET /api/v0/youtube/download?url=...` — download video and return MP4 bytes
- `POST /api/v0/short-url` — create a short url (JSON body `{"url", "name"?, "description"?}`), returns `{"id", "token"}`
- `GET /api/v0/short-url/{id}` — redirect (302) to the target url, 404 if unknown
- `DELETE /api/v0/short-url/{id}` — delete a short url; requires the delete `token` (`?token=` query param or JSON body), 403 on mismatch, 404 if unknown
- `GET /api/v0/uploaded-files` — list all uploaded files metadata
- `GET /api/v0/uploaded-files/{uuid}` — get metadata for a single uploaded file
- `GET /api/v0/uploaded-files/{uuid}/download` — download uploaded file content (returns 404 if expired or not found)
- `POST /api/v0/uploaded-files` — upload a file (multipart form data), returns metadata with `uuid` and `token`
- `DELETE /api/v0/uploaded-files/{uuid}` — delete uploaded file; requires the delete `token` (`?token=` query param), 403 on mismatch, 404 if unknown

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
