# Serbekun Services

A small self-hosted toolbox: a Java 21 / Javalin server exposing a set of
stateless developer utilities plus a few stateful ones (link repositories,
short URLs, burn links, file uploads), and a static front end for every one of
them.

**Live:** https://ss.serbekun.com/ · **API docs:** https://ss.serbekun.com/static/v0/html/api_docs.html

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Storage layout](#storage-layout)
- [HTTP API (v0)](#http-api-v0)
- [Front end](#front-end)
- [Project structure](#project-structure)
- [Building & deployment](#building--deployment)
- [Development & testing](#development--testing)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Cipher** — AES-256-GCM, RSA-2048 (OAEP-SHA256 + `SHA256withRSA` signatures),
  and hybrid encryption for payloads past RSA's ~190-byte ceiling. Stateless:
  keys travel in the request, nothing is written to disk.
- **Hash & integrity** — SHA-256, SHA-512, BLAKE3, HMAC-SHA256, over text or
  streamed file uploads, plus constant-time verification.
- **QR codes** — generate (PNG/SVG, size, colours, error correction) and read
  (upload, raw body, or base64), with `found: false` instead of an error when
  there is no code.
- **Encoding** — convert between `utf8`, `base64`, `base64url`, `base32`, `hex`,
  `url`, `url-form`.
- **Identifiers & randomness** — UUID v4/v7, monotonic ULIDs, random tokens and
  raw bytes, all from a single `SecureRandom`.
- **JSON tools** — validate, format, minify, query (JSON Pointer / JSONPath),
  and RFC 6902 diff.
- **Link repository** — named collections of links, each behind a token.
- **Short URLs** — create, redirect, delete, and render the short link as a QR
  code in one call.
- **Burn links** — text revealed exactly once and then destroyed, with optional
  TTL, device/browser filters, and IPv4/IPv6 + CIDR allow/deny lists.
- **Uploaded files** — upload with expiry, download, delete by token.
- **YouTube** — video metadata and download via `yt-dlp`.
- **Network** — client IP information.
- **Auth** — token gate supporting `Authorization` header and `?token=*** /
  `?Authorization=*** *** params.
- **Persistence** — JSON-file repositories under `repository/`, autosaved on a
  fixed interval; no external database.

## Requirements

- **Java 21**
- **Gradle wrapper** (`./gradlew`, Gradle 8.6) — do not install Gradle yourself
- Optional, only for the YouTube endpoints: `yt-dlp` and [Deno](https://deno.land/)
  on `PATH` (paths configurable)

## Quick start

```bash
# Compile + test
./gradlew build

# Build the fat JAR
./gradlew shadowJar
# -> build/libs/serbekun_services-<version>-all.jar

# Run (defaults to :8080; config is created on first start)
java -jar build/libs/serbekun_services-<version>-all.jar

# Tests only
./gradlew test
```

Then open http://localhost:8080/.

The build also generates `com/serbekun/ss/version.properties` from the Gradle
version, which is what `GET /api/v0/version` reports. Current version:
`alfa-2026-09-19`.

## Configuration

Runtime config lives at `repository/config.json`. It is created with defaults
on first start; edit it before booting to change anything.

| Key | Default | Meaning |
|-----|---------|---------|
| `port` | `8080` | HTTP bind port |
| `upload_file_max_size_mb` | `20` | Max upload size in MB (legacy `upload_file_max_size`, in bytes, is still read) |
| `youtube_process_timeout_seconds` | `30` | `yt-dlp` process timeout |
| `yt_dlp_path` | `/usr/local/bin/yt-dlp` | Path to `yt-dlp` |
| `deno_path` | `/usr/local/bin/deno` | Path to Deno |
| `repository_folder` | `repository` | Root of the on-disk storage |
| `links_repositories_storage_file` | `repository/repositories/links_repositories.json` | Link repositories |
| `tokens_storage_file` | `repository/endpoint_access_tokens.json` | Endpoint access tokens |
| `uploaded_files_raw_folder` | `repository/uploaded_files_raw/` | Raw uploaded bytes |
| `uploaded_files_storage_file` | `repository/uploaded_files/uploaded_files.json` | Upload metadata |
| `short_url_storage_file` | `repository/short_url/short_url.json` | Short URLs |
| `burn_link_storage_file` | `repository/burn_link/burn_link.json` | Burn links |
| `youtube_cookies_file` | `repository/www.youtube.com_cookies.txt` | YouTube cookies |

## Storage layout

Everything under `repository/` is created on startup and is git-ignored:

```
repository/
├── config.json
├── endpoint_access_tokens.json
├── repositories/
│   └── links_repositories.json
├── uploaded_files/
│   └── uploaded_files.json
├── uploaded_files_raw/            # raw file bytes
├── short_url/
│   └── short_url.json
├── burn_link/
│   └── burn_link.json
└── www.youtube.com_cookies.txt    # optional, for YouTube
```

Repositories are `AutoSavable` and flushed by a scheduler every **20 s**.
Uploaded files and burn links are swept by cleanup services every **600 s**
(lazy deletion also happens on access).

## HTTP API (v0)

All endpoints are currently public (`requiresAuth = false`); the token gate is in
place and can be enabled per-endpoint. Some routes additionally require their own
per-object token, passed as `?token=***

### System

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Landing page |
| `GET` | `/icon` | SVG favicon |
| `GET` | `/api/v0/version` | `{"version": "..."}` |
| `GET` | `/api/v0/network/ip` | Client IP information |
| `GET` | `/static/v0/{html,css,js,json,domain,images,svg,pdf}/{name}` | Static assets |

### Cipher

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v0/cipher/aes` | AES algorithm info |
| `POST` | `/api/v0/cipher/aes/encrypt` | AES-256-GCM encrypt |
| `POST` | `/api/v0/cipher/aes/decrypt` | AES-256-GCM decrypt |
| `GET` | `/api/v0/cipher/rsa/keypair` | Generate RSA-2048 key pair |
| `POST` | `/api/v0/cipher/rsa/encrypt` | RSA-OAEP-SHA256 encrypt (≤ 190 bytes) |
| `POST` | `/api/v0/cipher/rsa/decrypt` | RSA decrypt |
| `POST` | `/api/v0/cipher/rsa/sign` | `SHA256withRSA` sign |
| `POST` | `/api/v0/cipher/rsa/verify` | Verify signature (`valid: false` on mismatch) |
| `POST` | `/api/v0/cipher/hybrid/encrypt` | Hybrid encrypt (any size) |
| `POST` | `/api/v0/cipher/hybrid/decrypt` | Hybrid decrypt |

### Hash

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/hash` | Hash a payload (`sha256`, `sha512`, `blake3`, `hmac-sha256`) |
| `POST` | `/api/v0/hash/file` | Hash a streamed file upload; nothing stored |
| `POST` | `/api/v0/hash/verify` | Constant-time integrity check |

### QR

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/qr/generate` | Render a QR code (PNG bytes or SVG / JSON data URL) |
| `POST` | `/api/v0/qr/read` | Decode a QR code from an image |

### Encoding

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/encoding/{base64\|hex\|url}/encode` | Encode into that format |
| `POST` | `/api/v0/encoding/{base64\|hex\|url}/decode` | Decode from that format |
| `POST` | `/api/v0/encoding/convert` | Convert between any two formats |

### Identifiers & randomness

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v0/id/uuid?count=&version=&format=` | UUIDs (`v4`/`v7`) |
| `GET` | `/api/v0/id/ulid?count=&format=` | Monotonic ULIDs |
| `POST` | `/api/v0/id/batch` | Any mix of the above in one call |
| `GET` | `/api/v0/random/token?count=&length=&alphabet=&chars=` | Random strings |
| `GET` | `/api/v0/random/bytes?count=&length=&format=` | Raw randomness |

### JSON

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/json/validate` | Is the body valid JSON? |
| `POST` | `/api/v0/json/format?indent=&sort=` | Pretty-print (answers with the document) |
| `POST` | `/api/v0/json/minify?sort=` | Minify (answers with the document) |
| `POST` | `/api/v0/json/query?pointer=\|path=\|expression=&syntax=` | JSON Pointer / JSONPath query |
| `POST` | `/api/v0/json/diff` | RFC 6902 patch between two documents |

### Link repositories

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/repository/links/` | Create a repository; returns its token once |
| `GET` | `/api/v0/repository/links/{repositoryId}?token=*** | Get a repository and its links |
| `DELETE` | `/api/v0/repository/links/{repositoryId}?token=*** | Delete a repository |
| `POST` | `/api/v0/repository/links/{repositoryId}/links?token=*** | Add a link |
| `PUT` | `/api/v0/repository/links/{repositoryId}/links/{uuid}?token=*** | Update a link |
| `DELETE` | `/api/v0/repository/links/{repositoryId}/links/{uuid}?token=*** | Delete a link |

### Short URLs

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/short-url` | Create a short URL |
| `POST` | `/api/v0/short-url/qr` | Create a short URL and its QR code in one call |
| `GET` | `/api/v0/short-url/{id}` | 302 redirect to the target |
| `DELETE` | `/api/v0/short-url/{id}` | Delete (requires the delete token) |

### Burn links

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v0/burn` | Create a self-destructing link |
| `GET` | `/b/{id}` | Reveal page (button only — never burns) |
| `POST` | `/api/v0/burn/{id}/reveal` | Burn and return the text |
| `DELETE` | `/api/v0/burn/{id}` | Delete early (requires the delete token) |

> Missing, expired, already-burned and access-blocked links all return the same
> `404`, so a link cannot be probed to discover that a filter exists. The
> two-step reveal is deliberate: chat-app link previews only issue `GET`s and
> therefore cannot consume the secret before a human opens it.

### Uploaded files

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v0/uploaded-files` | List all file metadata |
| `GET` | `/api/v0/uploaded-files/max-size` | `{"megabytes", "bytes"}` |
| `GET` | `/api/v0/uploaded-files/{uuid}` | Metadata for one file |
| `GET` | `/api/v0/uploaded-files/{uuid}/download` | Download bytes (404 if expired) |
| `POST` | `/api/v0/uploaded-files` | Upload (multipart); returns `uuid` + `token` |
| `DELETE` | `/api/v0/uploaded-files/{uuid}?token=*** | Delete (requires the delete token) |

### YouTube

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v0/youtube/info?url=` | Video metadata as JSON |
| `GET` | `/api/v0/youtube/download?url=` | Video as MP4 bytes |

Requires `yt-dlp` and Deno. Playlist URLs are skipped (`--no-playlist`);
age-restricted videos need authenticated cookies.

## Front end

Static pages served from `src/main/resources/`, linked from the landing page:

- `/static/v0/html/cipher_aes.html` — AES
- `/static/v0/html/cipher_rsa.html` — RSA key pairs, encrypt/decrypt, sign/verify, hybrid
- `/static/v0/html/hash.html` — text / file / verify tabs
- `/static/v0/html/encoding.html` — live converter with swap
- `/static/v0/html/id.html` — UUIDs, ULIDs, tokens, bytes
- `/static/v0/html/json.html` — validate / format / minify / query / diff
- `/static/v0/html/links.html` — link repositories
- `/static/v0/html/short_url.html` — short URLs
- `/static/v0/html/qr.html` — generate / read / short-link QR
- `/static/v0/html/burn_link.html` — burn links
- `/static/v0/html/uploaded_files.html` — file uploads
- `/static/v0/html/youtube.html` — YouTube tools
- `/static/v0/html/ip_info.html` — client IP
- `/static/v0/html/pomodoro.html`, `/static/v0/html/quiz_ui.html` — standalone widgets
- `/static/v0/html/api_docs.html` — API documentation

## Project structure

Clean layered architecture, no DI framework — everything is wired by hand in
`Main`.

| Layer | Package | Role |
|-------|---------|------|
| Bootstrap | `com.serbekun.Main` | Entry point; manual wiring via inner context classes |
| Config | `com.serbekun.ss.config` | `Config` (JSON, with defaults) + `Paths` |
| Domain | `com.serbekun.ss.domain.*` | Entities and HTTP DTOs, framework-free |
| Repository | `com.serbekun.ss.repo.*` | Repository interfaces + JSON-file implementations (all `AutoSavable`) |
| Service | `com.serbekun.ss.service.*` | Business logic and orchestration |
| HTTP | `com.serbekun.ss.http.handles.*` | Javalin routes, DTO mapping, validation — thin, no business logic |
| Infrastructure | `com.serbekun.ss.infrastructure.*` | Filesystem init, schedulers |
| Resources | `com.serbekun.ss.resources.*` | Classpath resource loading and caching |

Deeper internals (auth model, per-feature design notes, test coverage, the full
handler list) live in [`AGENTS.md`](AGENTS.md), which is the canonical reference
for project conventions. To add an endpoint: implement `HttpHandler` and add it
to the list in `ServerFactory` — that list is the single source of truth and is
reused by the integration tests.

## Building & deployment

```bash
./gradlew shadowJar
java -jar build/libs/serbekun_services-<version>-all.jar
```

`sh_utils/deploy.sh` does the full deploy: builds the shadow JAR, `scp`s it to
the DMZ host as `~/serbekun_services/serbekun-service.jar`, provisions
`yt-dlp` + Deno there (`sh_utils/provision_dmz.sh`), and restarts the service in
its `tmux` session (`ss_serbekun_com`).

**Production:** run behind a reverse proxy with HTTPS — the public instance at
https://ss.serbekun.com/ is fronted by NGINX and Cloudflare. The app relies on
`CF-Connecting-IP` / `X-Forwarded-For` to see real client IPs (burn-link IP
rules depend on this).

## Development & testing

- Keep business rules in `service/`; HTTP handlers stay thin and free of logic.
- No DI framework — prefer explicit constructor wiring through `Main`.
- JSON-file storage only; no external database.
- Tests: **JUnit 5 + AssertJ + Mockito**, run with `./gradlew test`.
  - Unit tests per layer, in-memory repo + JSON `*FileRepo` round-trips
    (`@TempDir`), config loading/defaults, domain model validation.
  - HTTP integration tests drive the full route tree via `javalin-testtools`
    against real services over in-memory repos.
  - `YoutubeTest` needs `yt-dlp`, Deno and network access — expect failures
    without them.

## Contributing

Bug reports, issues and pull requests are welcome. Follow the existing layered
architecture and keep handlers thin. Open an issue first for non-trivial changes
or design discussions.

## License

MIT — see [LICENSE](LICENSE). © 2026 Mishchenko Sergey.

## Contact

Open an issue in this repository.
