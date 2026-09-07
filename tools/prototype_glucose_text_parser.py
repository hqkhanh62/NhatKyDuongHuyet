#!/usr/bin/env python3
# Design-validation artifact for the GlucoseScanner text parser changes
# (dropped decimal recovery, letter-token gating, colon decimals).
# It transliterates normalizeOcrText + extractGlucose 1:1 to Python and runs
# the JVM test corpus; keep in sync when the Kotlin parser changes.
# Python transliteration of GlucoseScanner's text parser (normalizeOcrText +
# extractGlucose) used to validate the regex changes on the JVM test corpus.
import re
import sys

MIN_GLUCOSE = 2.0
MAX_GLUCOSE = 30.0
MG_DL_PER_MMOL = 18.0

numberRegex = re.compile(
    r"(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])"
)
dateOrTimeRegex = re.compile(
    r"\b[0-9]{1,4}[/\-][0-9]{1,2}(?:[/\-][0-9]{1,4})?\b|"
    r"\b(?:[01]?\d|2[0-3]):[0-5]\d\b"
)


def normalize_numeric_token(token):
    return (token
            .replace("O", "0").replace("o", "0")
            .replace("Q", "0").replace("q", "0")
            .replace("I", "1").replace("i", "1")
            .replace("L", "1").replace("l", "1")
            .replace("|", "1"))


def normalize_ocr_text(text):
    normalized = text.replace("\u00A0", " ").replace("\u066B", ".").replace("\uFF0C", ".").replace(",", ".")
    normalized = re.sub(r"(?<=\d)\s*[.]\s*(?=\d)", ".", normalized)
    normalized = re.sub(r"(?<![0-9])([0-9])\s*:\s*([0-9])(?![0-9])", r"\1.\2", normalized)
    normalized = re.sub(r"(?<!\d)(\d{1,2})\s+(\d)(?=\s*(?:mmol|mg(?:/\s*dl)?|$))",
                        r"\1.\2", normalized, flags=re.IGNORECASE)
    return re.sub(r"[ \t]+", " ", normalized).strip()


class Candidate:
    def __init__(self, value, score, position):
        self.value = value
        self.score = score
        self.position = position


def extract_glucose(text):
    if not text.strip():
        return None
    candidates = []
    absolute_position = 0
    for line in normalize_ocr_text(text).splitlines():
        line_context = line.lower()
        has_mmol_unit = "mmol" in line_context
        has_mg_unit = "mg" in line_context
        has_glucose_label = any(w in line_context for w in ("glucose", "sugar", "result", "value"))

        if dateOrTimeRegex.search(line) and not has_glucose_label and not has_mmol_unit and not has_mg_unit:
            absolute_position += len(line) + 1
            continue

        for match in numberRegex.finditer(line):
            token = match.group(1)
            token_has_real_digit = any(c in "0123456789" for c in token)
            if not token_has_real_digit and not has_mmol_unit and not has_mg_unit:
                continue
            try:
                raw_value = float(normalize_numeric_token(token))
            except ValueError:
                continue

            if has_mg_unit:
                converted = raw_value / MG_DL_PER_MMOL
            elif has_mmol_unit and MAX_GLUCOSE < raw_value <= 350:
                converted = raw_value / 10.0
            elif raw_value > 20.0 and not has_mmol_unit:
                continue
            elif MIN_GLUCOSE <= raw_value <= MAX_GLUCOSE:
                converted = raw_value
            else:
                continue

            if not (MIN_GLUCOSE <= converted <= MAX_GLUCOSE):
                continue

            score = 0
            if has_mmol_unit: score += 100
            if has_mg_unit: score += 90
            if raw_value % 1.0 != 0.0: score += 25
            if 3.0 <= converted <= 20.0: score += 10
            if has_glucose_label: score += 20
            candidates.append(Candidate(converted, score, absolute_position + match.start()))

        absolute_position += len(line) + 1

    if not candidates:
        return None
    candidates.sort(key=lambda c: (-c.score, c.position))
    return candidates[0].value


# ---------------------------------------------------------------------------
# The full JVM test corpus (existing GlucoseScannerTest + new cases)
# ---------------------------------------------------------------------------
CASES = [
    # (expected, text)
    (6.1, "Glucose: 6,1 mmol/L"),
    (6.1, "Result 6 . 1 mmol/L"),
    (5.7, "5 7 mmol/L"),
    (10.1, "10 . 1 mmol/L"),
    (6.1, "Result 6 1 mmol/L"),
    (110.0 / 18.0, "Result: 110 mg/dL"),
    (6.2, "20/08/2026 08:32\nValue: 6.2 mmol/L"),
    (None, "Result: 81"),
    (5.7, "5.7 mmol/L"),
    (None, "28.0"),
    (6.1, "Glucose: 6.l mmol/L"),
    (None, "Glucose: 0.8 mmol/L"),
    # new cases
    (5.7, "57 mmol/L"),
    (10.1, "101 mmol/L"),
    (4.5, "Glucose: 45 mmol/L"),
    (None, "57"),
    (None, "Lo"),
    (None, "Result: Lo"),
    (10.0, "Result: I0"),
    (5.7, "5:7 mmol/L"),
    (6.2, "20/08/2026 08:32\nValue: 6.2 mmol/L"),
]


def main():
    failures = 0
    for expected, text in CASES:
        got = extract_glucose(text)
        ok = (got is None and expected is None) or (
            got is not None and expected is not None and abs(got - expected) < 0.001)
        status = "PASS" if ok else "FAIL"
        if not ok:
            failures += 1
        print(f"[{status}] {text!r} -> {got} (expected {expected})")
    if failures:
        sys.exit(1)
    print("ALL PASS")


if __name__ == "__main__":
    main()
