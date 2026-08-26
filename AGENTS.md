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

- **Unit tests** per layer: services (`cipher` — AES, RSA, hybrid, `hash`, `qr`, `encoding`, `id` (+ `Ulid`), `json` (+ `JsonDiff`), `shorturl`, `linksrepo`, `uploadedfiles`, `auth`, `resource`), in-memory repos + JSON `*FileRepo` persistence roundtrips (`@TempDir`), `Config` loading/defaults/legacy-field migration, domain model validation + Jackson roundtrips, `ResourceCache`/`ResourcesBasePath`.
- **HTTP integration tests** at `src/test/java/com/serbekun/ss/http/ServerHttpIntegrationTest.java` — full route tree via `RouteInitializer` + `javalin-testtools`, real services over in-memory repos; only the `Youtube` (yt-dlp) wrapper is mocked. Covers index/static, version, cipher, hash, qr, encoding, id/random, json, short-url, repository-links, uploaded-files (multipart), and youtube endpoints.
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
- `POST /api/v0/qr/generate` — render a QR code (JSON body `{"data", "format"?, "size"?, "errorCorrection"?, "foreground"?, "background"?, "margin"?}`); answers with the raw image, or with `{"format", "contentType", "size", "image"}` (a `data:` URL) when the caller sends `Accept: application/json` or `?json=true`
- `POST /api/v0/qr/read` — read a code out of an image (multipart `file`, a raw `image/*` body, or JSON `{"image": "<base64>"}`), returns `{"found", "text", "format"}`; an image with no code is a 200 with `found: false`, not an error
- `POST /api/v0/encoding/{base64|hex|url}/encode` — write a payload in that format (JSON body `{"data", "encoding"?, "form"?}`, where `encoding` says how to read the input, default `utf8`), returns `{"data", "bytes", "from", "to"}`
- `POST /api/v0/encoding/{base64|hex|url}/decode` — read a payload written in that format (JSON body `{"data", "outputEncoding"?, "form"?}`, where `outputEncoding` says how to write the result, default `utf8`), same response shape
- `POST /api/v0/encoding/convert` — convert between any two formats (JSON body `{"data", "from", "to"}`), same response shape; the six routes above are this one with a side pinned by the path
- `GET /api/v0/id/uuid?count=&version=&format=` — UUIDs, `v4` (default) or `v7`, in `canonical` / `compact` / `upper` / `urn`
- `GET /api/v0/id/ulid?count=&format=` — ULIDs, in `canonical` / `lower` / `uuid` / `hex`
- `POST /api/v0/id/batch` — every kind in one call (JSON body `{"items":[{"type", "count"?, …}]}`, or a single item unwrapped), returns `{"items":[…]}`
- `GET /api/v0/random/token?count=&length=&alphabet=&chars=` — random strings from `base62` (default), `base58`, `base64url`, `base32`, `hex`, `digits`, `lower`, `upper`, or a custom `chars` set
- `GET /api/v0/random/bytes?count=&length=&format=` — raw randomness in `hex` (default), `base64`, `base64url` or `base32`
- `POST /api/v0/json/validate` — is the body JSON? Returns `{"valid", "error"?, "line"?, "column"?, "bytes"}`; a broken document is a 200 with `valid: false`, not an error
- `POST /api/v0/json/format?indent=&sort=` — pretty-print; answers with the **document itself**, not a wrapper (`indent` is 1–16 spaces or `tab`)
- `POST /api/v0/json/minify?sort=` — strip every avoidable byte; also answers with the document
- `POST /api/v0/json/query?pointer=|path=|expression=&syntax=` — JSON Pointer or JSONPath, returns `{"expression", "syntax", "count", "matches", "paths"}`; no match is a 200 with `count: 0`
- `POST /api/v0/json/diff` — RFC 6902 patch between two documents (JSON body `{"from", "to"}`), returns `{"equal", "operations", "patch"}`
- `POST /api/v0/repository/links/` — create a link repository (JSON body `{"name"?}`), returns `{"repositoryId", "token", "name", "createdAt"}` (the `token` is shown only here)
- `GET /api/v0/repository/links/{repositoryId}?token=...` — get a repository with all its links, 404 if not found or token is invalid
- `DELETE /api/v0/repository/links/{repositoryId}?token=...` — delete a repository, 204 on success, 404 if not found or token is invalid
- `POST /api/v0/repository/links/{repositoryId}/links?token=...` — add a link (JSON body `{"url", "name"?, "description"?}`), returns `{"uuid", "url", "name", "description"}`
- `PUT /api/v0/repository/links/{repositoryId}/links/{uuid}?token=...` — update a link (JSON body `{"url", "name"?, "description"?}`), 204 on success, 400 if `url` is blank, 404 if not found or token is invalid
- `DELETE /api/v0/repository/links/{repositoryId}/links/{uuid}?token=...` — delete a link, 204 on success, 404 if not found or token is invalid
- `GET /api/v0/youtube/info?url=...` — get video metadata as JSON
- `GET /api/v0/youtube/download?url=...` — download video and return MP4 bytes
- `POST /api/v0/short-url` — create a short url (JSON body `{"url", "name"?, "description"?}`), returns `{"id", "token"}`
- `POST /api/v0/short-url/qr` — create a short url and render the short link as a QR code in one call; takes the short-url body plus the QR options and an optional `baseUrl`, returns `{"id", "token", "shortUrl", "format", "contentType", "size", "qr"}` where `qr` is a `data:` URL
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

## QR functionality

`service/qr/` — stateless generation and reading, nothing stored.

- `QrFormat` / `QrErrorCorrection` — the wire vocabulary (`png`/`svg`, `L`/`M`/`Q`/`H`) with loose
  parsing and one canonical `wireName()` echoed back, the same shape as `HashAlgorithm`.
- `QrService.QrOptions.of(...)` — parses and validates everything a caller can ask for (size 64–4096,
  margin 0–32, CSS-style hex colours including the `#rrggbbaa` alpha form) and fills in the defaults,
  so the HTTP layer stays a pass-through. Colours are packed ARGB internally.
- `QrService` — both formats come from the same ZXing module matrix. PNG is rendered at the requested
  pixel size through `MatrixToImageWriter`; **SVG is rendered from the unscaled module grid** (ZXing
  is asked for a 1×1 render, which yields exactly the modules plus the quiet zone) and scaled by its
  `viewBox`, so a 4096 px SVG is the same few kilobytes as a 512 px one. Dark modules are emitted as
  one path of horizontal runs, not a rect per module.
- Reading goes through `BufferedImageLuminanceSource` with `TRY_HARDER`, then retries on
  `source.invert()` — a code printed light-on-dark is ordinary and must still read. An image with no
  code returns `found: false` rather than throwing; bytes that are not a decodable image are a
  `400`. SVG cannot be read (`ImageIO` has no decoder), which the error message says.
- A transparent background (`#00000000`) leaves the backdrop out entirely: no `<rect>` in the SVG,
  an alpha-0 PNG.
- **ZXing** (`com.google.zxing:core` + `:javase`) is the only new dependency; `javase` is there for
  the `BufferedImage`/`ImageIO` bridge that reading needs.

`QrServiceTest` round-trips wherever it can — encode, then decode — so "looks like a QR code" cannot
pass. The hand-rolled SVG path is rasterized in the test and read back, because a wrong run length
would show up nowhere else.

Frontend: `html/qr.html` — generate (live preview, download, copy image / data url), read
(drag-and-drop **and** clipboard paste of a screenshot), and short link, which creates a short url
and its code in one call using the options set on the generate tab.

## Encoding functionality

`service/encoding/` — conversion between representations, stateless and nothing stored. It is
neither encryption nor hashing: every step is reversible by anyone, which the page and the docs say
out loud so the three are not confused.

- `EncodingFormat` — the seven formats (`utf8`, `base64`, `base64url`, `base32`, `hex`, `url`,
  `url-form`), each a two-way street between text and bytes. That is what makes one conversion
  enough for all seven endpoints: decode out of `from`, encode into `to`. `fromWire(name, field)`
  matches spelling loosely and names the offending field in its error.
- **Encoding is canonical, decoding is forgiving** — output is padded uppercase base32, lowercase
  hex, padded base64; input may carry whitespace, lowercase, missing padding, a `0x` prefix or `:`
  separators in hex, and the url-safe alphabet on the standard base64 route (a JWT segment decodes
  without picking a variant).
- **The two url variants are kept apart on purpose.** `url` is RFC 3986 (space → `%20`, `+` is a
  literal plus); `url-form` is `x-www-form-urlencoded` (space → `+`). Reading one as the other is
  the classic way `a+b` becomes `a b`, so the fixed url routes take a `form` boolean and `convert`
  takes the name.
- **UTF-8 output refuses rather than mangles** — the lenient decoder would answer `�` for arbitrary
  bytes, losing the data and hiding that it happened, so `EncodingFormat.UTF8.encode` throws and the
  HTTP layer answers `400` naming hex/base64/base32 as the fix.
- `Base32` is hand-rolled (the JDK ships none) — RFC 4648, no new dependency.
- `bytes` in every response is the size of the payload itself, never of the text it is written as.

`EncodingServiceTest` pins the RFC 4648 §10 vectors for base64, base32 and hex first — everything
else only proves self-consistency, those prove agreement with the rest of the world — then
round-trips 512 random bytes through every format.

Frontend: `html/encoding.html` — one live converter (from/to pickers, debounced conversion as you
type, byte count, copy) with a swap button that carries the output back into the input, so "encode
this" becomes "now decode it again" in one click.

## Identifiers and random material

`service/id/` — UUIDs, ULIDs, tokens and raw bytes, all from one `SecureRandom`. Nothing is stored.

- Every request is an `IdService.Spec` and every answer a `Result`, both holding wire values. That is
  why `/id/batch` is three lines: it is a list of exactly the specs the single-purpose endpoints
  build for themselves. Validation lives in the service, so an error reads the same whichever route
  it arrived through.
- **The `SecureRandom` is the point, not decoration** — these values end up as session tokens and
  delete keys. Token draws use `nextInt(bound)` rather than `nextInt() % size`, because the modulo
  form quietly favours the start of any alphabet whose size is not a power of two, which is most of
  them. A custom `chars` set that repeats a character is refused: it would be drawn twice as often
  and the reported `bits` would lie.
- `Ulid` is hand-rolled and **monotonic**: within one millisecond the 80-bit random half is
  incremented rather than redrawn, so a batch sorts correctly, and a clock that steps backwards
  keeps the last timestamp instead of issuing an id that sorts before one already handed out. A ULID
  that does not sort by time is just a slower UUID, so `UlidTest` pins both rules along with the two
  extreme encodings and a round trip through an independently written decoder.
- UUID v7 is built by hand (48-bit timestamp, version and variant nibbles, randomness around them);
  v4 comes from `UUID.randomUUID()`, which is itself `SecureRandom`-backed.
- `bits` in every response is the randomness one value carries, rounded **down** — 122 for v4, 74 for
  v7, 80 for a ULID, `floor(length × log2(alphabet))` for a token. Rounding a security margin up is
  how a token ends up weaker than the number beside it claims.
- `count` is capped at 1000, and on a batch the cap is on the **whole call** — a per-item limit would
  let a hundred items of a thousand values each straight through.
- Random bytes reuse `EncodingFormat` from `service/encoding/` for output; `utf8` is refused by name,
  since random bytes are not text.
- **Two endpoint registrations, one generator** — `/api/v0/id/*` issues identifiers meant to be
  shared, `/api/v0/random/*` issues secrets. Keeping them apart means the day one of them needs a
  token or a rate limit, it can have one without the other.

Frontend: `html/id.html` — one picker per kind with only that kind's options shown, click-a-value to
copy, and a note on each tab saying what the kind is actually for (v4 vs v7, why base58).

## JSON functionality

`service/json/` — validate, format, minify, query and diff. Stateless, nothing stored.

- **The request body is the document itself**, options ride in the query string. That is what makes
  these usable from a shell (`curl --data-binary @file.json`) and the only shape that can accept a
  *broken* document at all, since invalid JSON cannot be quoted inside a JSON envelope. `/diff` is
  the one exception — two documents need a wrapper. `format` and `minify` answer with the document
  rather than a field holding it, for the same reason.
- **Duplicate keys are an error** (`STRICT_DUPLICATE_DETECTION`). A lenient parser keeps the last one,
  which would make the formatter delete data while reporting success. A tool whose job is to tell you
  about your document has to say so instead.
- **Numbers keep their value exactly** — `USE_BIG_DECIMAL_FOR_FLOATS` plus
  `JsonNodeFactory.withExactBigDecimals(true)`, because the default factory strips trailing zeros and
  would quietly rewrite `1.0` as `1`. The text may still be normalised (`1e2` → `1E+2`), the value
  never is.
- The pretty printer is a `DefaultPrettyPrinter` subclass only because Jackson writes `"a" : 1` by
  default and every other formatter in the world writes `"a": 1`.
- `JsonDiff` emits `add`/`remove`/`replace` only — `move` and `copy` are optional in RFC 6902 and only
  shorten a patch. Numbers compare **by value** (`1` and `1.0` are one number per the RFC), which
  Jackson's own `equals` would call a change. Arrays of equal length are compared element-wise so a
  nested change stays nested; unequal lengths go through an LCS match, so one insertion into a
  thousand-element array is one operation. Past a million LCS cells it falls back to comparing by
  index and fixing the tail — still correct, just longer.
- Query: JSON Pointer is Jackson's own `JsonNode.at`; JSONPath is **jayway json-path** configured with
  the Jackson node provider (`ALWAYS_RETURN_LIST` for values, a second config with `AS_PATH_LIST` for
  where each match was). The syntax is detected from the expression — only a JSONPath starts with `$`,
  only a pointer starts with `/` — and `syntax` overrides the guess.

`JsonDiffTest` applies every generated patch with an applier written in the test, because a patch is
only correct if applying it yields the target; the rest of its cases check the patch is also *small*.

Frontend: `html/json.html` — five tabs over one document box (two in diff), a verdict line for
validate, the byte saving computed locally for minify, and "use as input" to chain format → query.

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
