# RepoChecker — Backend

Spring Boot REST API that clones **public** GitHub repositories and runs a security scan.

Works with the frontend (`repo_checker_fe`): the user submits a repo URL, the scan runs asynchronously in the background, and results are fetched by `jobId`.

## Features

- Validates public `github.com/owner/repo` URLs
- Shallow clone via `git clone --depth 1` (temporary directory, removed after the scan)
- Three scanners:
  - **SECRET** — AWS keys, GitHub PATs, Stripe keys, bearer tokens, hardcoded credential patterns
  - **ENV_FILE** — `.env` files and suspicious variable lines
  - **SUSPICIOUS_SCRIPT** — patterns such as `curl|sh`, `wget|sh`, `eval()`, `base64 --decode`
- Sensitive snippets are masked with `SnippetMasker`
- Concurrent scan limit and per-file size cap (`application.properties`)

## Requirements

- Java 17+
- Maven 3.9+ (or the project `./mvnw` wrapper)
- `git` available on the system `PATH`

## Running

```bash
cd repo_checker_be
./mvnw spring-boot:run
```

The API listens on **http://localhost:8080** by default.

## Configuration

`src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `scan.clone-timeout-seconds` | `120` | `git clone` timeout (seconds) |
| `scan.max-file-size-bytes` | `1048576` | Max size per scanned file (1 MB) |
| `scan.max-concurrent` | `5` | Parallel scan worker threads |
| `scan.temp-dir-prefix` | `repo-checker-` | Prefix for temporary clone directories |

## API

Base path: `/api/github`

### `POST /api/github/scan`

Starts a scan. Request body:

```json
{
  "repoUrl": "https://github.com/owner/repo"
}
```

Response (`202 Accepted`):

```json
{
  "jobId": "uuid",
  "status": "PENDING"
}
```

### `GET /api/github/scan/{jobId}`

Scan status and findings.

`status`: `PENDING` | `RUNNING` | `COMPLETED` | `FAILED`

Example completed response:

```json
{
  "jobId": "...",
  "repoUrl": "https://github.com/owner/repo",
  "status": "COMPLETED",
  "findingCount": 2,
  "findings": [
    {
      "severity": "CRITICAL",
      "category": "SECRET",
      "filePath": "config.js",
      "lineNumber": 12,
      "description": "Possible GitHub personal access token detected",
      "snippet": "ghp_****"
    }
  ]
}
```

On error: `error` field (`400`) or `errorMessage` on a `FAILED` job.

## Architecture (brief)

```
GitHubController → GitHubService → InMemoryScanJobStore
                              ↘ ScanOrchestrator (@Async)
                                    → RepoCloneService (git)
                                    → SecretScanner / EnvFileScanner / SuspiciousScriptScanner
```

- Jobs are stored in-memory in `InMemoryScanJobStore` (lost on restart).
- CORS allows `http://localhost:5173` and `http://127.0.0.1:5173` (Vite dev server).

## Development

```bash
./mvnw test
./mvnw package
```

## Limitations

- Only public GitHub HTTPS URLs are supported.
- Private repos and non-GitHub hosts are not supported.
- Scan rules are regex-based; false positives and false negatives are possible.
- Production use should add a persistent job store and rate limiting.

## Related project

UI: [repo_checker_fe](../repo_checker_fe) — Vite + React; proxies `/api` to `localhost:8080` in development.
