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
from typing import Any

from fastapi import FastAPI, HTTPException
from google import genai
from pydantic import BaseModel

app = FastAPI(title="Blood Glucose AI Insight Backend")

API_KEY = os.getenv("GEMINI_API_KEY", "").strip()
MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash").strip()

BASE_PROMPT = (
    "You are a concise, empathetic assistant for a personal blood glucose diary. "
    "Summarize the observed pattern, possible everyday influences, and practical "
    "blood-glucose monitoring reminders. Keep it to 3-6 short bullet points. "
    "NEVER give a medical diagnosis. NEVER prescribe or adjust medication. "
    "Always advise consulting a doctor for medical decisions."
)


class InsightRequest(BaseModel):
    history: str | None = None
    forecast: dict[str, Any] | None = None
    language: str = "vi"


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
def insight(req: InsightRequest) -> dict[str, str]:
    if not API_KEY:
        raise HTTPException(
            status_code=500, detail="Server Gemini key not configured."
        )

    try:
        text = call_gemini(build_prompt(req))
    except Exception as exc:  # noqa: BLE001 - surface upstream errors to the app
        raise HTTPException(
            status_code=502, detail=f"Upstream AI error: {exc}"
        ) from exc

    if not text:
        raise HTTPException(
            status_code=500, detail="AI returned an empty response."
        )
    return {"insight": text}