#!/usr/bin/env python3
# Design-validation artifact for the "Pro AI" camera pipeline.
#
# It transliterates the pure-Kotlin logic of
#   ml/MeterTextParser.kt                      (OCR da tang: so / gio / ngay / loi may do)
#   domain/scanner/GlucoseSession.kt           (tu dong phan loai buoi)
#   domain/scanner/AutoImportPipeline.kt       (slot truoc/sau an, Auto Clean)
# so that the regexes, the date-ambiguity rules and the import policy can be
# exercised on a corpus before (and beside) the JVM test suite
# MeterTextParserTest / AutoImportPipelineTest.
#
# Keep in sync when the Kotlin implementation changes.
import datetime
import re
import sys
import calendar

MIN_GLUCOSE = 2.0
MAX_GLUCOSE = 30.0
MG_DL_PER_MMOL = 18.0

# --------------------------------------------------------------- tier 0: text
CONFUSABLES = {0x066B: ".", 0xFF0C: ".", ord(","): ".", ord("\u00a0"): " "}

# A single digit on each side only: real times (08:32, 8:30) keep their colon.
COLON_DECIMAL = re.compile(r"(?<![0-9])([0-9])\s*:\s*([0-9])(?![0-9])")


def normalize_display_text(text: str) -> str:
    normalized = text.translate(CONFUSABLES)
    normalized = COLON_DECIMAL.sub(r"\1.\2", normalized)
    return re.sub(r"[ \t]+", " ", normalized).strip()


# --------------------------------------------------- tier 1: the glucose value
NUMBER = re.compile(r"(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])")
DATE_OR_TIME_ROW = re.compile(
    r"\b[0-9]{1,4}[/\-][0-9]{1,2}(?:[/\-][0-9]{1,4})?\b|"
    r"\b(?:[01]?\d|2[0-3]):[0-5]\d\b"
)
SPACE_DECIMAL = re.compile(r"(?<!\d)(\d{1,2})\s+(\d)(?=\s*(?:mmol|mg(?:/\s*dl)?|$))", re.IGNORECASE)
SPACED_DOT = re.compile(r"(?<=\d)\s*[.]\s*(?=\d)")
NOISE_WORDS = ("day", "avg", "date", "time", "mem", "max", "min")


def normalize_numeric_token(token: str) -> str:
    table = {"O": "0", "o": "0", "Q": "0", "q": "0", "I": "1", "i": "1", "L": "1", "l": "1", "|": "1"}
    return "".join(table.get(ch, ch) for ch in token)


def line_score(line: str):
    """Returns [(value, score)] for one OCR line; mirrors MeterTextParser."""
    context = line.lower()
    has_mmol = "mmol" in context
    has_mg = "mg" in context
    has_label = any(w in context for w in ("glucose", "sugar", "result", "value"))
    if DATE_OR_TIME_ROW.search(line) and not (has_mmol or has_mg or has_label):
        return []
    normalized = SPACED_DOT.sub(".", SPACE_DECIMAL.sub(r"\1.\2", line))

    out = []
    for index, match in enumerate(NUMBER.finditer(normalized)):
        token = match.group(1)
        if not any(ch.isdigit() for ch in token) and not (has_mmol or has_mg):
            continue
        try:
            raw = float(normalize_numeric_token(token))
        except ValueError:
            continue
        if has_mg:
            value = raw / MG_DL_PER_MMOL
        elif has_mmol and MAX_GLUCOSE < raw <= 350.0:
            value = raw / 10.0
        elif raw > 20.0 and not has_mmol:
            continue
        elif MIN_GLUCOSE <= raw <= MAX_GLUCOSE:
            value = raw
        else:
            continue
        if not (MIN_GLUCOSE <= value <= MAX_GLUCOSE):
            continue
        score = 0
        if has_mmol:
            score += 100
        if has_mg:
            score += 90
        if raw % 1 != 0:
            score += 25
        if 3.0 <= value <= 20.0:
            score += 10
        if has_label:
            score += 20
        out.append((round(value, 3), score, index))
    return out


def extract_glucose(text: str):
    if not text.strip():
        return None
    candidates = []
    for line in normalize_display_text(text).splitlines():
        candidates.extend(line_score(line))
    if not candidates:
        return None
    return sorted(candidates, key=lambda c: (-c[1], c[2]))[0][0]


def extract_glucose_from_lines(lines):
    """lines = [(text, boxHeightPx)]: spatial selection of the big reading."""
    scored = []
    for index, (text, height) in enumerate(lines):
        found = line_score(normalize_display_text(text))
        if not found:
            continue
        value = max(found, key=lambda c: (c[1], -c[2]))[0]
        lower = text.lower()
        score = min(height, 1000) + max(c[1] for c in found)
        if "." in text or "," in text:
            score += 180
        if "mmol" in lower or "mg" in lower:
            score += 300
        if any(w in lower for w in NOISE_WORDS):
            score -= 500
        scored.append((score, -index, value))
    if not scored:
        return None
    return max(scored, key=lambda s: (s[0], s[1]))[2]


# --------------------------------------------------------- tier 2: display time
TIME_LABEL = re.compile(r"(time|giờ|gio|clock)", re.IGNORECASE)
COLON_TIME = re.compile(r"(?<![\d:.])((?:[01]?\d|2[0-3])):([0-5]\d)(?::([0-5]\d))?(?![\d:])")
LABELLED_TIME = re.compile(r"(?<![\d:.])((?:[01]?\d|2[0-3]))[.:]([0-5]\d)(?::([0-5]\d))?(?![\d:])")
MERIDIEM_TIME = re.compile(r"(?<!\d)(1[0-2]|[1-9])[:.]([0-5]\d)\s*([AP])\.?\s*\.?M?(?![0-9A-Za-z])", re.IGNORECASE)


def extract_time(text: str):
    """Returns (hour, minute, confidence) or None."""
    best = None
    for offset, line in enumerate(normalize_display_text(text).splitlines()):
        labelled = bool(TIME_LABEL.search(line))
        patterns = (LABELLED_TIME,) if labelled else (COLON_TIME,)
        if not labelled:
            meridiem = MERIDIEM_TIME.search(line)
            if meridiem:
                hour = int(meridiem.group(1)) % 12
                if meridiem.group(3).upper() == "P":
                    hour += 12
                best = _better_time(best, (hour, int(meridiem.group(2)), 0.85, 0, offset))
        for match in patterns[0].finditer(line):
            hour, minute = int(match.group(1)), int(match.group(2))
            if hour > 23 or minute > 59:
                continue
            penalty = 30 if match.group(3) else 0
            best = _better_time(best, (hour, minute, 1.0 if labelled else 0.8, penalty, offset))
    if best is None:
        return None
    return (best[0], best[1], best[2])


def _better_time(current, candidate):
    if current is None:
        return candidate
    rank = lambda c: (c[2], -c[3], -c[4])  # noqa: E731
    return candidate if rank(candidate) > rank(current) else current


# -------------------------------------------------------- tier 3: display date
DATE_LABEL = re.compile(r"(date|ngày|ngay|dd/mm|yyyy)", re.IGNORECASE)
ISO_DATE = re.compile(r"(?<!\d)(\d{4})([-/.])(\d{1,2})\2(\d{1,2})(?!\d)")
FULL_DATE = re.compile(r"(?<!\d)(\d{1,2})([-/.])(\d{1,2})\2(\d{2,4})(?!\d)")
SHORT_DATE = re.compile(r"(?<!\d)(\d{1,2})([-/])(\d{1,2})(?!\d)")
SHORT_DOT = re.compile(r"(?<!\d)(\d{2})[.](\d{2})(?!\d)")
MAX_FUTURE_DAYS = 1


def _valid(year: int, month: int, day: int) -> bool:
    return 2000 <= year <= 2100 and 1 <= month <= 12 and 1 <= day <= calendar.monthrange(year, month)[1]


def _expand_year(token: str) -> int:
    value = int(token)
    return 2000 + value if len(token) <= 2 and value <= 79 else (1900 + value if len(token) <= 2 else value)


def _resolve(day_token, month_token, year, allow_swap):
    """Returns (year, month, day, ambiguous, swapped) or None."""
    a, b = int(day_token), int(month_token)
    if a > 12 and b <= 12:
        day, month, ambiguous = a, b, False
    elif b > 12 and a <= 12:
        day, month, ambiguous = b, a, False
    else:
        day, month, ambiguous = a, b, True
    if _valid(year, month, day):
        return (year, month, day, ambiguous, False)
    if allow_swap and _valid(year, day, month):
        return (year, day, month, True, True)
    return None


def _strip_times(line: str) -> str:
    return COLON_TIME.sub(" ", line)


def extract_date(text: str, fallback_year: int, today_iso: str):
    """Returns (isoDate, confidence, ambiguous) or None (Auto Clean aware).

    Tier order: YYYY-MM-DD, then D/M/YYYY (day first unless the second number
    can only be a day), then year-less DD/MM. A year-less dot form is honoured
    only when the line is labelled, so "5.7" or "8.30" never become dates.
    """
    best = None
    for offset, raw_line in enumerate(normalize_display_text(text).splitlines()):
        line = _strip_times(raw_line)
        labelled = bool(DATE_LABEL.search(line))
        found = []
        for match in ISO_DATE.finditer(line):
            year, month, day = int(match.group(1)), int(match.group(3)), int(match.group(4))
            if _valid(year, month, day):
                found.append((year, month, day, 0.95 if labelled else 0.9, False))
            elif _valid(year, day, month):
                found.append((year, day, month, 0.7, True))
        for match in FULL_DATE.finditer(line):
            year = _expand_year(match.group(4))
            for day, month, ambiguous, swapped in _orient(match.group(1), match.group(3)):
                if _valid(year, month, day):
                    conf = 0.7 if swapped else (0.9 if labelled else 0.85)
                    found.append((year, month, day, conf, ambiguous))
                    break
        if not found:
            for first, _sep, third, base_conf in _short_dates(line, labelled):
                for day, month, ambiguous, swapped in _orient(first, third):
                    if _valid(fallback_year, month, day):
                        found.append((fallback_year, month, day, 0.7 if swapped else base_conf, ambiguous))
                        break
        for year, month, day, conf, ambiguous in found:
            iso = f"{year:04d}-{month:02d}-{day:02d}"
            if _out_of_plausible_window(iso, today_iso):
                continue
            score = (conf, -offset)
            if best is None or score > best[0]:
                best = (score, iso, conf, bool(ambiguous))
    if best is None:
        return None
    return (best[1], best[2], best[3])


def _orient(first, third):
    """Candidate (day, month) orders, day-first: the vi-VN default."""
    a, b = int(first), int(third)
    if a > 12 and b <= 12:
        return [(a, b, False, False)]
    if b > 12 and a <= 12:
        return [(b, a, False, False)]
    return [(a, b, True, False), (b, a, True, True)]


def _short_dates(line, labelled):
    """Year-less pairs; at least one side must be two digits."""
    out = []
    for match in SHORT_DATE.finditer(line):
        first, sep, third = match.group(1), match.group(2), match.group(3)
        if len(first) < 2 and len(third) < 2:
            continue
        out.append((first, sep, third, 0.9 if labelled else 0.6))
    if not out and labelled:
        for match in SHORT_DOT.finditer(line):
            out.append((match.group(1), ".", match.group(2), 0.45))
    return out


def _out_of_plausible_window(iso: str, today_iso: str) -> bool:
    try:
        value = datetime.date.fromisoformat(iso)
        today = datetime.date.fromisoformat(today_iso)
    except ValueError:
        return True
    return value > today + datetime.timedelta(days=MAX_FUTURE_DAYS) or value.year < today.year - 5


# ------------------------------------------------------------ meter error codes
ERROR_CODE = re.compile(r"(?<![0-9A-Za-z])(E[\s-]?\d{1,3}|ERR(?:OR)?|HI|LO)(?![0-9A-Za-z])", re.IGNORECASE)


def detect_meter_error(text: str):
    """Error codes are only honoured on lines without a readable value."""
    for line in normalize_display_text(text).splitlines():
        lower = line.lower()
        if any(w in lower for w in ("mmol", "mg", "glucose", "result", "value")) and line_score(line):
            continue
        match = ERROR_CODE.search(line)
        if not match:
            continue
        code = re.sub(r"[\s-]", "", match.group(1)).upper()
        if code.startswith("ERR"):
            return "ERR"
        if code.startswith("E"):
            return "E-" + code[1:]
        return code
    return None


# -------------------------------------------------- auto import pipeline policy
def session_for_hour(hour: int) -> str:
    if 5 <= hour <= 10:
        return "Sáng"
    if 11 <= hour <= 13:
        return "Trưa"
    if 14 <= hour <= 17:
        return "Chiều"
    return "Tối"


MEAL_ANCHOR_MINUTES = {"Sáng": 6 * 60 + 30, "Trưa": 11 * 60 + 30,
                       "Chiều": 11 * 60 + 30, "Tối": 18 * 60 + 30}


def preferred_slot(hour: int, minute: int, session: str):
    """Trước ăn / Sau ăn theo giờ đo; None ngoài hai cửa sổ bữa ăn."""
    minutes = hour * 60 + minute
    anchor = MEAL_ANCHOR_MINUTES[session]
    if anchor - 75 <= minutes <= anchor + 30:
        return "BEFORE"
    if anchor + 60 <= minutes <= anchor + 180:
        return "AFTER"
    return None


def decide_slot(hour: int, minute: int, session: str, has_before: bool, has_after: bool) -> str:
    pref = preferred_slot(hour, minute, session)
    if pref == "BEFORE" and not has_before:
        return "BEFORE"
    if pref == "AFTER" and not has_after:
        return "AFTER"
    if not has_before:
        return "BEFORE"
    if not has_after:
        return "AFTER"
    return "AFTER"


def clean_value(raw):
    if raw is None:
        return None, "NO_VALUE"
    if not isinstance(raw, (int, float)) or raw != raw or raw in (float("inf"), float("-inf")):
        return None, "NOT_FINITE"
    quantized = round(float(raw), 1)
    if quantized < MIN_GLUCOSE or quantized > MAX_GLUCOSE:
        return None, "OUT_OF_RANGE"
    return quantized, None


# --------------------------------------------------------------------- corpus
GLUCOSE_REGRESSION = [
    (6.1, "Glucose: 6,1 mmol/L"),
    (6.1, "Result 6 . 1 mmol/L"),
    (5.7, "5 7 mmol/L"),
    (10.1, "10 . 1 mmol/L"),
    (6.1, "Result 6 1 mmol/L"),
    (110 / 18, "Result: 110 mg/dL"),
    (6.2, "20/08/2026 08:32\nValue: 6.2 mmol/L"),
    (None, "Result: 81"),
    (5.7, "5.7 mmol/L"),
    (None, "28.0"),
    (6.1, "Glucose: 6.l mmol/L"),
    (None, "Glucose: 0.8 mmol/L"),
    (5.7, "57 mmol/L"),
    (10.1, "101 mmol/L"),
    (4.5, "Glucose: 45 mmol/L"),
    (None, "57"),
    (None, "Lo"),
    (None, "Result: Lo"),
    (10.0, "Result: I0"),
    (5.7, "5:7 mmol/L"),
]

SPATIAL_CASES = [
    (5.7, [("DAY", 40), ("5.7", 200), ("mmol/L", 45), ("08:32", 40)]),
    (6.4, [("AVG 5.9", 40), ("6.4 mmol/L", 210), ("15/09", 38)]),
    (None, [("DAY", 40), ("AVG", 38)]),
    (8.8, [("8.8", 240), ("AVG 6.1", 40)]),
]

TIME_CASES = [
    ((8, 32, 1.0), "Date 20/08/2026\nTime 08:32\n6.2 mmol/L"),
    ((8, 32, 0.8), "20/08/2026 08:32\nValue: 6.2 mmol/L"),
    ((22, 5, 0.8), "22:05\n5.4 mmol/L"),
    ((7, 30, 0.85), "7:30 AM 5.9 mmol/L"),
    ((19, 45, 0.85), "7.45 PM 8.8 mmol/L"),
    ((9, 15, 0.8), "09:15:30 6.6 mmol/L"),
    ((6, 0, 1.0), "Time: 06:00"),
    (None, "6.2 mmol/L"),
    (None, "1.2.3"),
]

DATE_CASES = [
    (("2026-08-20", 0.95, False), "Date 2026-08-20\n6.2 mmol/L"),
    (("2026-08-20", 0.85, False), "20/08/2026 08:32\nValue: 6.2 mmol/L"),
    (("2026-08-22", 0.85, False), "08/22/2026 6.2 mmol/L"),
    (("2026-09-08", 0.85, True), "08/09/2026 6.2"),
    (("2026-09-15", 0.6, False), "15/09 6.2 mmol/L"),
    (("2026-05-13", 0.7, True), "2026-13-05\n6.2 mmol/L"),
    (("2026-08-20", 0.85, False), "20.08.2026 6.2 mmol/L"),
    (None, "6.2 mmol/L"),
    (None, "5.7"),
    (None, "2030-01-01 6.2 mmol/L"),
    (("2026-08-30", 0.45, False), "Date 30.08"),
]

SESSION_CASES = [
    (5, "Sáng"), (9, "Sáng"), (10, "Sáng"), (11, "Trưa"), (13, "Trưa"),
    (14, "Chiều"), (17, "Chiều"), (18, "Tối"), (23, "Tối"), (0, "Tối"), (4, "Tối"),
]

SLOT_CASES = [
    # (hour, minute, session, hasBefore, hasAfter, expected)
    (6, 40, "Sáng", False, False, "BEFORE"),
    (8, 55, "Sáng", True, False, "AFTER"),
    (9, 0, "Sáng", False, False, "AFTER"),
    (13, 45, "Trưa", True, False, "AFTER"),
    (14, 30, "Chiều", True, False, "AFTER"),
    (17, 0, "Chiều", False, False, "BEFORE"),
    (20, 30, "Tối", True, True, "AFTER"),
    (7, 0, "Sáng", True, True, "AFTER"),
]

ERROR_CASES = [
    ("E-05", "E-05\nInsert strip"),
    ("HI", "HI"),
    ("LO", "LO"),
    (None, "6.2 mmol/L"),
    (None, "Result: 6.2 mmol/L\n08:32"),
]


def main() -> int:
    failures = 0

    def check(label, got, expected):
        nonlocal failures
        ok = got == expected
        if not ok:
            failures += 1
        print(f"[{'PASS' if ok else 'FAIL'}] {label}: got {got!r} expected {expected!r}")

    print("== tier 1: glucose (JVM regression corpus) ==")
    for expected, text in GLUCOSE_REGRESSION:
        got = extract_glucose(text)
        ok = (got is None and expected is None) or (
            got is not None and expected is not None and abs(got - expected) < 0.01)
        if not ok:
            failures += 1
        print(f"[{'PASS' if ok else 'FAIL'}] {text!r} -> {got} (expected {expected})")

    print("\n== tier 1: spatial line selection ==")
    for expected, lines in SPATIAL_CASES:
        check("spatial", extract_glucose_from_lines(lines), expected)

    print("\n== tier 2: time ==")
    for expected, text in TIME_CASES:
        check(f"time {text!r}", extract_time(text), expected)

    print("\n== tier 3: date ==")
    for expected, text in DATE_CASES:
        check(f"date {text!r}", extract_date(text, 2026, "2026-09-15"), expected)

    print("\n== auto session classification ==")
    for hour, expected in SESSION_CASES:
        check(f"hour {hour}", session_for_hour(hour), expected)

    print("\n== before/after meal slot ==")
    for hour, minute, session, hb, ha, expected in SLOT_CASES:
        check(f"slot {hour:02d}:{minute:02d}/{session}",
              decide_slot(hour, minute, session, hb, ha), expected)

    print("\n== auto clean ==")
    check("5.7 kept", clean_value(5.7), (5.7, None))
    check("1.9 rejected", clean_value(1.9), (None, "OUT_OF_RANGE"))
    check("30.1 rejected", clean_value(30.1), (None, "OUT_OF_RANGE"))
    check("30.0 kept", clean_value(30.0), (30.0, None))
    check("3.14 -> 3.1", clean_value(3.14), (3.1, None))
    check("nan rejected", clean_value(float("nan")), (None, "NOT_FINITE"))

    print("\n== meter error codes ==")
    for expected, text in ERROR_CASES:
        check(f"error {text!r}", detect_meter_error(text), expected)

    if failures:
        print(f"\n{failures} FAILURE(S)")
        return 1
    print("\nALL PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
