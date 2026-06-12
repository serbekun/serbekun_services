# serbekun-service

## Instructions for AI agents

When the user states any preferences about coding style, architecture, conventions, or project direction, **append them to this file** so the information persists across chat sessions. If something is unclear or contradicts existing content, ask the user for clarification before changing code. This file is the canonical reference for project conventions — keep it up to date.

## Quick start

```sh
gradle build                 # compile + test
gradle shadowJar             # fat JAR → build/libs/serbekun-service-1.0.0-all.jar
java -jar build/libs/serbekun-service-1.0.0-all.jar  # server on :8080
gradle test                  # JUnit 5
gradle compileJava           # compile only
```

No lint, typecheck, or codegen tasks exist. No CI/CD.

## Project structure

| Layer          | Package                              | Role |
|----------------|--------------------------------------|------|
| Bootstrap      | `com.serbekun.Main`                  | Entry point. Manual dependency wiring via inner `ServerContext` / `Repositories` / `Services` / `Resources` / `Handlers` classes. No DI framework. |
| Config         | `com.serbekun.ss.config`             | `Config` (port), `Paths` — hardcoded static configuration. |
| Domain         | `com.serbekun.ss.domain.models`      | Pure domain entities: `Link`, `Links`, `LocalTokens`, `EndpointsAccessTokens`. No framework dependencies. |
| Repository     | `com.serbekun.ss.repository`         | Repository **interfaces** + JSON-file implementations (Jackson). All implement `AutoSavable`. |
| Service        | `com.serbekun.ss.service.*`          | Business logic and orchestration only (auth, cipher, links, resources, tokens, autosave, youtube). |
| HTTP (thin)    | `com.serbekun.ss.http.handles.*`     | Javalin route registration, DTOs (Request/Response), validation, mapping, and service calls. **No business logic**. |
| Infrastructure | `com.serbekun.ss.infrastructure.*`   | Low-level technical components (FS initialization, autosave scheduler). |
| Resources      | `com.serbekun.ss.resources.*`        | Load and cache static files from classpath (`src/main/resources/`). |

## Architecture notes

- **Clean Layered Architecture** — full refactoring completed in 2026. The previous anti-pattern `service/http/handles/` (business logic mixed with HTTP) has been completely removed.
- **Domain** is isolated (`domain/models/`). All domain models are plain Java classes with Jackson annotations only where needed for persistence.
- **Repository interfaces** exist for all data access (`LinksRepository`, `LocalTokensRepository`, `EndpointAccessTokensRepository`). Implementations are in `*Impl` classes.
- **Services** contain all business rules and orchestration. They depend only on repository interfaces and other services. `Youtube` is a utility service (not wired into the DI context).
- **HTTP layer is thin** — only handles routing, DTO mapping, basic validation, and delegates to services.
- **No DI framework** — manual constructor injection via inner context classes in `Main.java`.
- **File-based storage** — all data persists as JSON under `repository/` (auto-created on startup). The file `repository/endpoint_access_tokens.json` holds auth tokens.
- **Auth model** — endpoints registered via `EndpointRegistry`. Javalin `before` handler supports `Authorization` header and `?token=` / `?Authorization=Bearer ...` query params. `requiresAuth` is currently `false` for all endpoints.
- **Autosave** — `ScheduledExecutorService` calls `save()` on all `AutoSavable` repositories every **20 seconds**.
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

Single unit test at `src/test/java/com/serbekun/ss/service/resource/ResourceServiceTest.java` — uses Mockito + AssertJ. No integration tests. Run with `./gradlew test`.

## API endpoints (v0)

All registered in `http/handles/*Routes` classes:

- `GET /` — index page
- `GET /static/v0/images/{name}` — static images
- `GET /static/v0/json` / `GET /static/v0/json/{name}` — static JSON
- `GET /static/v0/html/{name}` — static HTML pages
- `GET /api/v0/cipher/aes` — cipher info
- `POST /api/v0/cipher/aes/encrypt` — AES encrypt
- `POST /api/v0/cipher/aes/decrypt` — AES decrypt
- `GET /api/v0/catalogs/links` — list links
- `POST /api/v0/catalogs/links` — create link
- `PUT /api/v0/catalogs/links/{uuid}` — update link
- `DELETE /api/v0/catalogs/links/{uuid}` — delete link
- `GET /api/v0/youtube/info?url=...` — get video metadata as JSON
- `GET /api/v0/youtube/download?url=...` — download video and return MP4 bytes

## YouTube functionality

- **Requires**: `yt-dlp` command-line tool installed and in PATH
- **Cookies**: Uses `repository/www.youtube.com_cookies.txt` for YouTube authentication
- **Current limitations**:
  - Age-restricted videos require authenticated cookies from a logged-in browser
  - Playlist URLs (with `&list=` parameters) are skipped (`--no-playlist` flag)
  - Videos with complex signature validation may require JavaScript runtime (deno/node) and yt-dlp-ejs package
  
### Implementation notes (2026-06-12)
- Added 120-second timeout to prevent yt-dlp processes from hanging indefinitely
- Uses `--no-playlist` flag to ignore playlist parameters in URLs
- Process timeout with `waitFor(timeout, TimeUnit.SECONDS)` and `destroyForcibly()` on timeout
- yt-dlp can work without explicit JS runtime for many videos, falls back to basic extraction
