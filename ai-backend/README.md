# AI insight backend (reference implementation)

Minimal FastAPI service that forwards glucose history to Google Gemini and
returns a concise, safe summary to the Android app. Contract documented in
[`docs/gemini-insights-backend.md`](../docs/gemini-insights-backend.md).

- The Gemini API key lives **only on this backend** — never in the app.
- No auth is enforced here because the current app client does not send
  credentials. Do not expose this endpoint to the public internet without
  adding authentication and rate limiting (see Security below).

## Local development

```bash
pip install -r requirements.txt
export GEMINI_API_KEY=...            # Windows PowerShell: $env:GEMINI_API_KEY="..."
uvicorn main:app --host 0.0.0.0 --port 8000
```

Smoke test:

```bash
curl -s http://localhost:8000/health
curl -s -X POST http://localhost:8000/api/gemini/insight ^
  -H "Content-Type: application/json" ^
  -d "{\"history\":\"2026-08-13 08:00: 5.8 mmol/L\",\"language\":\"vi\"}"
```

## Deploying to get a stable HTTPS URL

The Android `BuildConfig.GEMINI_BACKEND_URL` is fixed at build time, so you
need a stable public URL:

1. **Render (free)** — create a new *Web Service* from this `ai-backend`
   folder, or a new private repo containing it.
   - Build command: `pip install -r requirements.txt`
   - Start command: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - Environment: set `GEMINI_API_KEY` (and optionally `GEMINI_MODEL`).
2. **Railway / Fly.io** — same app, same two commands/variables.
3. Or run locally and expose a temporary HTTPS tunnel
   (`cloudflared tunnel --url http://localhost:8000`) — the URL changes on
   every restart, which is fine for testing only.

## Pointing the Android app at the backend

In the repo root:

```properties
# local.properties
GEMINI_BACKEND_URL=https://your-deployed-host.example.com/api/gemini/insight
```

Then rebuild the debug APK:

```bash
gradlew assembleDebug
```

The app will call `POST <GEMINI_BACKEND_URL>` and show the returned
`insight` in the "Phân tích AI" card on the dashboard.

## Security checklist (required before public release)

- Add authentication (the app client currently sends no token — update
  `GeminiBackendClient.kt` to attach one, then enforce it here).
- Rate-limit per user/device and verify the caller before calling Gemini.
- Obtain explicit user consent before transmitting health measurements.
- Keep HTTPS-only, minimize logs, define retention/deletion rules.
- Sanitize prompt input and response output; the returned text is
  informational, never a diagnosis.