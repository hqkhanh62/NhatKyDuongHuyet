"""Minimal Gemini insight backend for the NhatKyDuongHuyet Android app.

Contract (see docs/gemini-insights-backend.md):
  POST <GEMINI_BACKEND_URL>  with JSON body
    { "history": "...", "forecast": {...} | null, "language": "vi" | "en" }
  and expect HTTP 2xx + { "insight": "..." }.

Local run:
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000

Deployment and app configuration: see README.md in this directory.
"""

import os
import hashlib
import hmac
import time
from collections import defaultdict, deque
from typing import Any

from fastapi import FastAPI, Header, HTTPException, Request
from google import genai
from pydantic import BaseModel, Field

app = FastAPI(title="Blood Glucose AI Insight Backend")

API_KEY = os.getenv("GEMINI_API_KEY", "").strip()
MODEL = os.getenv("GEMINI_MODEL", "gemini-3.6-flash").strip()
BACKEND_TOKEN = os.getenv("GEMINI_BACKEND_TOKEN", "").strip()
RATE_LIMIT = max(1, int(os.getenv("GEMINI_RATE_LIMIT_PER_MINUTE", "10")))
RATE_WINDOW_SECONDS = 60.0
MAX_PROMPT_CHARS = 24_000
_requests_by_client: dict[str, deque[float]] = defaultdict(deque)

BASE_PROMPT = (
    "You are a concise, empathetic assistant for a personal blood glucose diary. "
    "Summarize the observed pattern, possible everyday influences, and practical "
    "blood-glucose monitoring reminders. Keep it to 3-6 short bullet points. "
    "NEVER give a medical diagnosis. NEVER prescribe or adjust medication. "
    "Always advise consulting a doctor for medical decisions."
)


class InsightRequest(BaseModel):
    history: str | None = Field(default=None, max_length=20_000)
    forecast: dict[str, Any] | None = None
    language: str = Field(default="vi", pattern="^(vi|en)$")


def authorize(authorization: str | None) -> None:
    if not BACKEND_TOKEN:
        raise HTTPException(status_code=503, detail="Backend auth is not configured.")
    scheme, _, supplied = (authorization or "").partition(" ")
    if scheme.lower() != "bearer" or not hmac.compare_digest(supplied, BACKEND_TOKEN):
        raise HTTPException(status_code=401, detail="Unauthorized.")


def enforce_rate_limit(client_id: str) -> None:
    now = time.monotonic()
    bucket = _requests_by_client[client_id]
    while bucket and now - bucket[0] >= RATE_WINDOW_SECONDS:
        bucket.popleft()
    if len(bucket) >= RATE_LIMIT:
        raise HTTPException(status_code=429, detail="Rate limit exceeded.")
    bucket.append(now)


def build_prompt(req: InsightRequest) -> str:
    language = "English" if req.language == "en" else "Tiếng Việt (Vietnamese)"
    parts = [
        f"Please respond in {language}.",
        "Recent blood glucose entries:",
        req.history or "(no history)",
    ]
    if req.forecast:
        parts += [
            "On-device forecast:",
            f"hourly: {req.forecast.get('hourlyForecasts')}",
            f"expected range: {req.forecast.get('minExpected')} - "
            f"{req.forecast.get('maxExpected')} mmol/L",
        ]
    parts.append(BASE_PROMPT)
    return "\n\n".join(parts)


def call_gemini(prompt: str) -> str:
    client = genai.Client(api_key=API_KEY)
    response = client.models.generate_content(model=MODEL, contents=prompt)
    return (response.text or "").strip()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/gemini/insight")
def insight(
    req: InsightRequest,
    request: Request,
    authorization: str | None = Header(default=None),
) -> dict[str, str]:
    authorize(authorization)
    client_host = request.client.host if request.client else "unknown"
    # Hash the address before retaining it in process memory.
    client_id = hashlib.sha256(client_host.encode("utf-8")).hexdigest()
    enforce_rate_limit(client_id)
    if not API_KEY:
        raise HTTPException(
            status_code=500, detail="Server Gemini key not configured."
        )

    prompt = build_prompt(req)
    if len(prompt) > MAX_PROMPT_CHARS:
        raise HTTPException(status_code=413, detail="Request is too large.")

    try:
        text = call_gemini(prompt)
    except Exception as exc:  # noqa: BLE001 - surface upstream errors to the app
        raise HTTPException(
            status_code=502, detail=f"Upstream AI error: {exc}"
        ) from exc

    if not text:
        raise HTTPException(
            status_code=500, detail="AI returned an empty response."
        )
    return {"insight": text}
