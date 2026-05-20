# RepoChecker — Backend

GitHub üzerindeki **public** depoları klonlayıp güvenlik taraması yapan Spring Boot REST API.

Frontend (`repo_checker_fe`) ile birlikte çalışır: kullanıcı bir repo URL’si girer, tarama arka planda asenkron çalışır, sonuçlar `jobId` ile sorgulanır.

## Özellikler

- Public `github.com/owner/repo` URL doğrulama
- `git clone --depth 1` ile sığ klonlama (geçici dizin, tarama sonrası silinir)
- Üç tarayıcı:
  - **SECRET** — AWS anahtarı, GitHub PAT, Stripe anahtarı, bearer token, hardcoded credential kalıpları
  - **ENV_FILE** — `.env` dosyaları ve şüpheli değişken satırları
  - **SUSPICIOUS_SCRIPT** — `curl|sh`, `wget|sh`, `eval()`, `base64 --decode` gibi kalıplar
- Hassas snippet’ler `SnippetMasker` ile maskelenir
- Eşzamanlı tarama limiti ve dosya boyutu üst sınırı (`application.properties`)

## Gereksinimler

- Java 17+
- Maven 3.9+ (veya projedeki `./mvnw`)
- Sistemde `git` komutu (`PATH` üzerinde)

## Çalıştırma

```bash
cd repo_checker_be
./mvnw spring-boot:run
```

API varsayılan olarak **http://localhost:8080** üzerinde dinler.

## Yapılandırma

`src/main/resources/application.properties`:

| Özellik | Varsayılan | Açıklama |
|--------|------------|----------|
| `scan.clone-timeout-seconds` | `120` | `git clone` zaman aşımı (saniye) |
| `scan.max-file-size-bytes` | `1048576` | Taranan tek dosya üst sınırı (1 MB) |
| `scan.max-concurrent` | `5` | Paralel tarama iş parçacığı sayısı |
| `scan.temp-dir-prefix` | `repo-checker-` | Geçici klon dizini öneki |

## API

Temel path: `/api/github`

### `POST /api/github/scan`

Taramayı başlatır. Gövde:

```json
{
  "repoUrl": "https://github.com/owner/repo"
}
```

Yanıt (`202 Accepted`):

```json
{
  "jobId": "uuid",
  "status": "PENDING"
}
```

### `GET /api/github/scan/{jobId}`

Tarama durumu ve bulgular.

`status`: `PENDING` | `RUNNING` | `COMPLETED` | `FAILED`

Örnek tamamlanmış yanıt:

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

Hata durumunda `error` alanı (`400`) veya `errorMessage` (`FAILED` job).

## Mimari (kısa)

```
GitHubController → GitHubService → InMemoryScanJobStore
                              ↘ ScanOrchestrator (@Async)
                                    → RepoCloneService (git)
                                    → SecretScanner / EnvFileScanner / SuspiciousScriptScanner
```

- İşler bellek içi `InMemoryScanJobStore`’da tutulur (yeniden başlatmada kaybolur).
- CORS: `http://localhost:5173` ve `http://127.0.0.1:5173` (Vite dev sunucusu).

## Geliştirme

```bash
./mvnw test
./mvnw package
```

## Sınırlamalar

- Yalnızca public GitHub HTTPS URL’leri desteklenir.
- Özel depolar veya GitHub dışı hostlar desteklenmez.
- Tarama kuralları regex tabanlıdır; yanlış pozitif/negatif sonuçlar mümkündür.
- Üretim için kalıcı job store ve rate limiting düşünülmelidir.

## İlgili proje

Arayüz: [repo_checker_fe](../repo_checker_fe) — Vite + React, geliştirmede `/api` isteklerini `localhost:8080`’e proxy eder.
