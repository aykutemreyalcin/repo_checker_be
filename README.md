# RepoChecker — Backend

REST API for scanning **public** GitHub repositories for common security issues. Part of the RepoChecker project; pairs with the [frontend](../repo_checker_fe).

## Overview

1. Client sends a public `github.com/owner/repo` URL.
2. API creates a scan job (`PENDING`) and returns a `jobId` immediately (`202 Accepted`).
3. `ScanOrchestrator` runs asynchronously: shallow-clones the repo, runs three scanners, stores findings, deletes the clone directory.
4. Client polls `GET /api/github/scan/{jobId}` until status is `COMPLETED` or `FAILED`.

## Tech stack

- Java 17
- Spring Boot 4.0.6 (`spring-boot-starter-webmvc`, `spring-boot-starter-validation`)
- `git` CLI for cloning (`git clone --depth 1`)
- In-memory job store (no database)

## Requirements

| Tool | Notes |
|------|--------|
| Java 17+ | See `java.version` in `pom.xml` |
| Maven | Or use `./mvnw` |
| `git` | Must be on `PATH` |

## Quick start

```bash
cd repo_checker_be
./mvnw spring-boot:run
```

Server: **http://localhost:8080**

Example:

```bash
curl -s -X POST http://localhost:8080/api/github/scan \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"https://github.com/owner/repo"}'

curl -s http://localhost:8080/api/github/scan/<jobId>
```

## Configuration

`src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `scan.clone-timeout-seconds` | `120` | Max wait for `git clone` |
| `scan.max-file-size-bytes` | `1048576` | Skip files larger than 1 MB |
| `scan.max-concurrent` | `5` | Async scan thread pool size |
| `scan.temp-dir-prefix` | `repo-checker-` | Prefix for temp clone dirs |

## API

Base path: `/api/github`

### `POST /api/github/scan`

Starts a scan.

**Request**

```json
{
  "repoUrl": "https://github.com/owner/repo"
}
```

Validation (`400` + `{ "error": "..." }`):

- `repoUrl` required
- Must match `https?://github.com/{owner}/{repo}` (optional `.git` suffix)

**Response** — `202 Accepted`

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING"
}
```

### `GET /api/github/scan/{jobId}`

Returns job status and findings.

**Response** — `200 OK` (job found) or `404` (unknown `jobId`)

| Field | Description |
|-------|-------------|
| `status` | `PENDING`, `RUNNING`, `COMPLETED`, or `FAILED` |
| `findingCount` | Number of findings |
| `findings` | List when `COMPLETED` |
| `errorMessage` | Set when `FAILED` |

**Finding object**

| Field | Values |
|-------|--------|
| `severity` | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `category` | `SECRET`, `ENV_FILE`, `SUSPICIOUS_SCRIPT` |
| `filePath` | Path relative to repo root |
| `lineNumber` | Line number, or omitted |
| `description` | Human-readable rule hit |
| `snippet` | Masked excerpt (secrets never returned in full) |

## Scanners

### `SecretScanner` (`SECRET`)

Walks text files (binary skipped, `.git` skipped). Detects patterns including:

- AWS access keys (`AKIA...`)
- GitHub PATs (`ghp_...`)
- Stripe keys (`sk_live_...`, `sk_test_...`)
- Generic credential assignments (`api_key=`, `password=`, etc.)
- Bearer tokens

### `EnvFileScanner` (`ENV_FILE`)

Targets `.env`, `*.env`, and similar paths. Reports:

- **HIGH** — env file present in the repo
- **CRITICAL** — sensitive keys with non-placeholder values, or lines matching known secret prefixes
- Placeholders like `changeme`, `your_*`, `xxx`, `todo` are ignored

### `SuspiciousScriptScanner` (`SUSPICIOUS_SCRIPT`)

Checks `.sh`, `.bash`, `.ps1`, `.js`, `.ts`, and paths under `scripts/`. Flags patterns such as:

- `curl ... | sh` / `wget ... | sh`
- `eval(`
- `base64 --decode`

Severity: **MEDIUM**.

Findings are sorted by severity, then file path.

## Project layout

```
src/main/java/repoChecker/repo_checker_be/
  RepoCheckerBeApplication.java   # @EnableAsync
  config/
    AsyncConfig.java              # scan thread pool
    CorsConfig.java               # localhost:5173 for Vite
  controller/
    GitHubController.java
    GlobalExceptionHandler.java
  service/
    GitHubService.java
  scan/
    ScanOrchestrator.java
    RepoCloneService.java
    SecretScanner.java
    EnvFileScanner.java
    SuspiciousScriptScanner.java
    FileWalker.java
  store/
    InMemoryScanJobStore.java
  model/                          # ScanJob, Finding, enums
  dto/                            # API request/response records
  util/
    RepoUrlParser.java
    SnippetMasker.java
```

## Scan flow

```
POST /scan
  → GitHubService.startSecurityScan()
  → InMemoryScanJobStore.create()
  → ScanOrchestrator.runAsync()
       → RepoCloneService (git clone --depth 1)
       → run scanners on repo root
       → cleanup temp directory
  → GET /scan/{jobId} reads job from store
```

## Development

```bash
./mvnw test
./mvnw package
java -jar target/repo_checker_be-0.0.1-SNAPSHOT.jar
```

CORS is enabled for `http://localhost:5173` and `http://127.0.0.1:5173` so the Vite dev server can call the API directly (the frontend also proxies `/api` in development).

## Limitations

- **Public repos only** — private repos and non-GitHub hosts are rejected.
- **Regex-based rules** — expect false positives and false negatives.
- **In-memory jobs** — all jobs are lost on restart; not suitable for production without a persistent store.
- **No authentication** — add auth and rate limiting before exposing publicly.

## Related project

[repo_checker_fe](../repo_checker_fe) — React UI that starts scans and polls for results.
