# serbekun-service

## Quick start

```sh
./gradlew build                 # compile + test
./gradlew shadowJar             # fat JAR → build/libs/serbekun-service-1.0.0-all.jar
java -jar build/libs/serbekun-service-1.0.0-all.jar  # server on :8080
./gradlew test                  # JUnit 5
./gradlew compileJava           # compile only
```

No lint, typecheck, or codegen tasks exist. No CI/CD.

## Project structure

| Layer | Package | Role |
|-------|---------|------|
| Bootstrap | `com.serbekun.Main` | Entry point. Builds dependency graph via inner `ServerContext`/`Repositories`/`Services`/`Resources`/`Handlers` classes. No DI framework. |
| Config | `com.serbekun.ss.config.core.CoreConfig` | Hardcoded static paths under `repository/` dir. No env vars or external config files. |
| Domain | `com.serbekun.ss.core` | `Config`, `Links`, `EndpointsAccessTokens`, `LocalTokens`, `Programs` — plain data holders with `synchronized` methods. |
| Repository | `com.serbekun.ss.repository` | JSON-file persistence via Jackson. All implement `AutoSavable`. |
| Service | `com.serbekun.ss.service.*` | Business logic (auth, autosave, cipher, links, resource, tokens). |
| HTTP handlers | `com.serbekun.ss.http.handles.*` | Javalin route registration + request/response handling. |
| Service handlers | `com.serbekun.ss.service.http.handles.*` | Pure logic wrappers called by HTTP handlers (split exists but both are active). |
| Resources | `com.serbekun.ss.resources.*` | Load/cache static files from classpath (`src/main/resources/`). |

## Architecture notes

- **No DI framework** — manual wiring in `Main.java` through inner context holder classes.
- **File-based storage** — all data persists as JSON under `repository/` (auto-created on startup). The file `repository/endpoint_access_tokens.json` holds auth tokens.
- **Auth model** — endpoints are registered as `Endpoint("/path")` objects via `EndpointRegistry`. Javalin `before` handler checks `Authorization` header or `?token=`/`?Authorization=Bearer ...` query param. `requiresAuth` is currently set to `false` for all endpoints.
- **Autosave** — `ScheduledExecutorService` calls `save()` on all registered repositories every **20 seconds**.
- **Server port** — read from `repository/config.json` at startup. Defaults to `8080` if file does not exist (auto-created).
- **Java 21** with `-parameters` compiler flag (method parameter names preserved for Jackson).
- **Dead dependencies in `build.gradle`**: H2, Flyway, Hibernate Validator, Guava are declared but unused in source code.

## URLs

For example, the project is a Javalin 6 server with token-based auth, JSON-file persistence, and static resource serving. There are 56 source files, 1 test, and no CI/CD.

## Tests

Single unit test at `src/test/java/com/serbekun/ss/service/resource/ResourceServiceTest.java` — uses Mockito + AssertJ. No integration tests. Run with `./gradlew test`.

## API endpoints (v0)

All registered in `ss.http.handles.*Routes` classes:

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
