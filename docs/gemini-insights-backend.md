# Gemini insights backend contract

The Android app never contains a Gemini API key. It sends a user-approved request to the HTTPS endpoint configured as `GEMINI_BACKEND_URL` (or Gradle property `geminiBackendUrl`).

## Request

`POST <GEMINI_BACKEND_URL>` with `Content-Type: application/json`:

```json
{
  "history": "2026-08-13 08:00: 5.8 mmol/L",
  "forecast": {
    "hourlyForecasts": [6.1, 6.4, 6.2, 6.0],
    "minExpected": 6.0,
    "maxExpected": 6.4
  }
}
```

`forecast` is `null` when there are not enough valid measurements for an on-device LSTM forecast.

## Successful response

```json
{ "insight": "- ..." }
```

The backend must return HTTP 2xx and a non-empty `insight` string.

## Security and privacy requirements

- Keep the Gemini credential only on the backend and rotate the old credential that was embedded in prior APKs.
- Require authenticated requests, rate-limit per user/device, and verify the caller before accessing Gemini.
- Obtain explicit user consent before transmitting health measurements.
- Minimize logs, encrypt data in transit, and define retention/deletion rules appropriate to health data.
- Sanitize prompt input and response output; do not present generated text as medical diagnosis.