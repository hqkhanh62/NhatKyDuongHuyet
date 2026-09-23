#!/usr/bin/env python3
# Prototype of the seven-segment decoder that will be ported to
# app/src/main/java/com/example/nhatkyduonghuyet/ml/SevenSegmentDecoder.kt
#
# This script mirrors the planned Kotlin implementation (same constants, same
# steps) and validates it against synthetic renders of glucose meter displays,
# including ghosts, glare, labels, dark backlight and dropped decimal points.
import sys

MIN_GLUCOSE = 2.0
MAX_GLUCOSE = 30.0

# --- constants (must match SevenSegmentDecoder.kt) ---
MIN_DIM = 16
OTSU_MIN = 8
OTSU_MAX = 247
DARK_BG_MEAN = 110.0
GRAY_ZONE = 15.0
SEGMENT_ON_THRESHOLD = 0.30
DIGIT_CONFIDENCE = 0.70
READING_CONFIDENCE = 0.78
NO_DECIMAL_PENALTY = 0.90
NARROW_ONE_ASPECT = 2.9
MIN_COMPONENT_AREA_FRAC = 0.00002
MIN_COMPONENT_H_FRAC = 0.045
DIGIT_MIN_AREA_PX = 8
MAX_CLOSING_RADIUS = 5

# Canonical seven-segment geometry (fractions of the glyph box) used both by
# the synthetic renderer and by the classifier's sampling rectangles.
RENDER = {
    "A": (0.08, 0.000, 0.92, 0.110),
    "B": (0.890, 0.130, 1.000, 0.445),
    "C": (0.890, 0.555, 1.000, 0.870),
    "D": (0.08, 0.890, 0.92, 1.000),
    "E": (0.000, 0.555, 0.110, 0.870),
    "F": (0.000, 0.130, 0.110, 0.445),
    "G": (0.08, 0.445, 0.92, 0.555),
}

# Sampling rectangles sit strictly INSIDE the rendered segment bounds so that
# adjacent segments cannot bleed into each other's sample.
SAMPLE = {
    "A": (0.20, 0.040, 0.80, 0.090),
    "B": (0.860, 0.170, 0.970, 0.400),
    "C": (0.860, 0.600, 0.970, 0.830),
    "D": (0.20, 0.910, 0.80, 0.960),
    "E": (0.030, 0.600, 0.140, 0.830),
    "F": (0.030, 0.170, 0.140, 0.400),
    "G": (0.20, 0.470, 0.80, 0.530),
}

PATTERNS = {
    0: set("ABCDEF"),
    1: set("BC"),
    2: set("ABGED"),
    3: set("ABCDG"),
    4: set("FGBC"),
    5: set("AFGCD"),
    6: set("AFGEDC"),
    7: set("ABC"),
    8: set("ABCDEFG"),
    9: set("ABCDFG"),
}


def luminance(p):
    r = (p >> 16) & 0xFF
    g = (p >> 8) & 0xFF
    b = p & 0xFF
    return r * 0.299 + g * 0.587 + b * 0.114


def otsu(hist, total):
    sum_all = 0.0
    for i in range(256):
        sum_all += i * hist[i]
    sum_b = 0.0
    w_b = 0.0
    best_var = -1.0
    best_t = 127
    for t in range(256):
        w_b += hist[t]
        if w_b == 0:
            continue
        w_f = total - w_b
        if w_f == 0:
            break
        sum_b += t * hist[t]
        m_b = sum_b / w_b
        m_f = (sum_all - sum_b) / w_f
        var = w_b * w_f * (m_b - m_f) * (m_b - m_f)
        if var > best_var:
            best_var = var
            best_t = t
    return best_t


def closing(binary, w, h, r):
    return erode(dilate(binary, w, h, r), w, h, r)


def dilate(src, w, h, r):
    tmp = [0] * (w * h)
    dst = [0] * (w * h)
    for y in range(h):
        base = y * w
        for x in range(w):
            v = 0
            for k in range(-r, r + 1):
                xx = x + k
                if 0 <= xx < w and src[base + xx]:
                    v = 1
                    break
            tmp[base + x] = v
    for x in range(w):
        for y in range(h):
            v = 0
            for k in range(-r, r + 1):
                yy = y + k
                if 0 <= yy < h and tmp[yy * w + x]:
                    v = 1
                    break
            dst[y * w + x] = v
    return dst


def erode(src, w, h, r):
    tmp = [0] * (w * h)
    dst = [0] * (w * h)
    for y in range(h):
        base = y * w
        for x in range(w):
            v = 1
            for k in range(-r, r + 1):
                xx = x + k
                if not (0 <= xx < w) or not src[base + xx]:
                    v = 0
                    break
            tmp[base + x] = v
    for x in range(w):
        for y in range(h):
            v = 1
            for k in range(-r, r + 1):
                yy = y + k
                if not (0 <= yy < h) or not tmp[yy * w + x]:
                    v = 0
                    break
            dst[y * w + x] = v
    return dst


class Comp:
    __slots__ = ("min_x", "min_y", "max_x", "max_y", "area")

    def __init__(self):
        self.min_x = 1 << 30
        self.min_y = 1 << 30
        self.max_x = -1
        self.max_y = -1
        self.area = 0

    def w(self):
        return self.max_x - self.min_x + 1

    def h(self):
        return self.max_y - self.min_y + 1

    def cx(self):
        return (self.min_x + self.max_x) / 2.0

    def cy(self):
        return (self.min_y + self.max_y) / 2.0


def connected_components(binary, w, h):
    labels = [0] * (w * h)
    comps = []
    for start in range(w * h):
        if not binary[start] or labels[start]:
            continue
        comp = Comp()
        cid = len(comps) + 1
        stack = [start]
        labels[start] = cid
        while stack:
            idx = stack.pop()
            x = idx % w
            y = idx // w
            comp.area += 1
            if x < comp.min_x: comp.min_x = x
            if x > comp.max_x: comp.max_x = x
            if y < comp.min_y: comp.min_y = y
            if y > comp.max_y: comp.max_y = y
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if dx == 0 and dy == 0:
                        continue
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h:
                        nidx = ny * w + nx
                        if binary[nidx] and not labels[nidx]:
                            labels[nidx] = cid
                            stack.append(nidx)
        comps.append(comp)
    return comps


class Rect:
    __slots__ = ("l", "t", "r", "b")

    def __init__(self, l, t, r, b):
        self.l, self.t, self.r, self.b = l, t, r, b


def ratio_in(binary, gray, w, x0, y0, x1, y1):
    x0 = max(0, min(x0, w - 1)); x1 = max(x0 + 1, min(x1, w))
    y0 = max(0, min(y0, y1)); y1 = max(y0 + 1, min(y1, len(binary) // w))
    n = (x1 - x0) * (y1 - y0)
    if n <= 0:
        return 0.0, 0.0
    on = 0
    gray_count = 0
    for y in range(y0, y1):
        base = y * w
        for x in range(x0, x1):
            if binary[base + x]:
                on += 1
            elif gray[base + x]:
                gray_count += 1
    return on / n, gray_count / n


def classify_glyph(binary, gray, w, g):
    cw = g.w()
    ch = g.h()
    if ch / max(1, cw) >= NARROW_ONE_ASPECT:
        return 1, 0.95
    active = set()
    max_gray = 0.0
    for name, (sx, sy, ex, ey) in SAMPLE.items():
        x0 = int(g.min_x + sx * cw)
        y0 = int(g.min_y + sy * ch)
        x1 = int(g.min_x + ex * cw)
        y1 = int(g.min_y + ey * ch)
        on, gr = ratio_in(binary, gray, w, x0, y0, x1, y1)
        if on >= SEGMENT_ON_THRESHOLD:
            active.add(name)
            max_gray = max(max_gray, gr)
    if not active:
        return None
    best_digit, best_score = None, -1e9
    for digit, pattern in PATTERNS.items():
        inter = len(pattern & active)
        missing = len(pattern - active)
        extra = len(active - pattern)
        score = inter - 0.8 * missing - 0.6 * extra
        if score > best_score:
            best_score, best_digit = score, digit
    union = len(PATTERNS[best_digit] | active)
    conf = (len(PATTERNS[best_digit] & active) / union) if union else 0.0
    # Segments sitting in the Otsu gray zone make 8-vs-0 / 5-vs-6 ambiguous:
    # penalize so borderline digits get rejected instead of misread.
    if max_gray > 0.20:
        conf *= 0.8
    if conf < DIGIT_CONFIDENCE:
        return None
    return best_digit, conf


class Glyph:
    def __init__(self, comp):
        self.min_x = comp.min_x
        self.min_y = comp.min_y
        self.max_x = comp.max_x
        self.max_y = comp.max_y
        self.comps = [comp]

    def add(self, c):
        self.min_x = min(self.min_x, c.min_x)
        self.min_y = min(self.min_y, c.min_y)
        self.max_x = max(self.max_x, c.max_x)
        self.max_y = max(self.max_y, c.max_y)
        self.comps.append(c)

    def w(self):
        return self.max_x - self.min_x + 1

    def h(self):
        return self.max_y - self.min_y + 1

    def cx(self):
        return (self.min_x + self.max_x) / 2.0


def decode(pixels, w, h):
    if w < MIN_DIM or h < MIN_DIM:
        return None
    total = w * h
    hist = [0] * 256
    lums = [0.0] * total
    for i, p in enumerate(pixels):
        v = int(luminance(p))
        lums[i] = v
        hist[min(255, max(0, v))] += 1
    thr = otsu(hist, total)
    if thr < OTSU_MIN or thr > OTSU_MAX:
        return None  # flat frame (blur / glare) -> refuse to guess
    mean = sum(lums) / total
    dark_bg = mean < DARK_BG_MEAN

    binary = [0] * total
    gray = [0] * total
    for i, v in enumerate(lums):
        if dark_bg:
            binary[i] = 1 if v > thr else 0
            # weakly-lit bright segment: just below the threshold
            gray[i] = 1 if thr - GRAY_ZONE <= v <= thr else 0
        else:
            binary[i] = 1 if v <= thr else 0
            # weakly-lit dark segment: just above the threshold
            gray[i] = 1 if thr < v <= thr + GRAY_ZONE else 0

    r = max(1, min(MAX_CLOSING_RADIUS, min(w, h) // 70))
    binary = closing(binary, w, h, r)

    comps = connected_components(binary, w, h)
    if not comps:
        return None

    min_area = max(DIGIT_MIN_AREA_PX, int(MIN_COMPONENT_AREA_FRAC * total))
    min_h = max(10, int(MIN_COMPONENT_H_FRAC * h))
    candidates = [c for c in comps
                  if c.area >= min_area and c.h() >= min_h and c.w() >= 2]
    if not candidates:
        return None

    digit_h = max(c.h() for c in candidates)

    # Group candidate parts into glyphs by horizontal overlap only. A distance
    # rule is unsafe: a narrow "1" leaves only the inter-cell gap between it
    # and the next digit. Parts of one glyph must also be vertically close:
    # label text lit together with the digits (backlit meters) sits well below
    # the digit band and must not be merged into a glyph.
    candidates.sort(key=lambda c: c.min_x)
    groups = []
    for c in candidates:
        placed = False
        for g in groups:
            overlap = min(g.max_x, c.max_x) - max(g.min_x, c.min_x) + 1
            min_w = min(g.w(), c.w())
            v_gap = max(g.min_y - c.max_y, c.min_y - g.max_y)
            v_close = v_gap <= 0.5 * min(g.h(), c.h())
            if min_w > 0 and overlap >= 0.35 * min_w and v_close:
                g.add(c)
                placed = True
                break
        if not placed:
            groups.append(Glyph(c))
    if len(groups) < 2:
        return None

    groups.sort(key=lambda g: g.min_x)

    # Real glyph height (a "7" or "1" is two stacked parts joined here).
    glyph_h = max(g.h() for g in groups)
    digit_glyphs = [g for g in groups if g.h() >= 0.6 * glyph_h]
    if len(digit_glyphs) < 2:
        return None

    band_top = min(g.min_y for g in digit_glyphs)
    band_bottom = max(g.max_y for g in digit_glyphs)

    # Decimal-point blobs: tiny components at the baseline that did not become
    # part of any digit glyph.
    max_dec = 0.30 * glyph_h
    in_digit_glyph = set()
    for g in groups:
        if g in digit_glyphs:
            for c in g.comps:
                in_digit_glyph.add(id(c))
    decimals = [c for c in comps
                if id(c) not in in_digit_glyph
                and c.w() <= max_dec and c.h() <= max_dec
                and c.area >= 4]

    # A valid decimal needs a digit on both sides (meters: dd.dd style).
    decimals_after = [False] * len(digit_glyphs)
    any_decimal = False
    for d in decimals:
        if d.cy() < band_top + 0.55 * glyph_h or d.cy() > band_bottom + 0.15 * glyph_h:
            continue
        best_i = -1
        best_gap = 1e9
        for i, g in enumerate(digit_glyphs):
            if g.cx() < d.cx() and d.cx() - g.cx() < 0.8 * glyph_h:
                gapv = d.cx() - g.cx()
                if gapv < best_gap:
                    best_gap, best_i = gapv, i
        if 0 <= best_i < len(digit_glyphs) - 1 and digit_glyphs[best_i + 1].cx() > d.cx():
            decimals_after[best_i] = True
            any_decimal = True

    readings = []
    for g in digit_glyphs:
        res = classify_glyph(binary, gray, w, g)
        if res is None:
            return None  # a digit we cannot read confidently -> refuse frame
        readings.append(res)

    raw = ""
    for i, (digit, _) in enumerate(readings):
        raw += str(digit)
        if decimals_after[i]:
            raw += "."

    value = None
    try:
        value = float(raw)
    except ValueError:
        value = None

    if value is not None and not (MIN_GLUCOSE <= value <= MAX_GLUCOSE):
        # Integer out of mmol range: meters show one decimal digit, so this is
        # almost certainly a decimal blob we failed to detect (57 -> 5.7).
        recovered = None
        if raw.isdigit() and len(raw) >= 2:
            recovered_raw = raw[:-1] + "." + raw[-1]
            try:
                recovered = float(recovered_raw)
            except ValueError:
                recovered = None
        value = recovered

    if value is None:
        return None
    if not (MIN_GLUCOSE <= value <= MAX_GLUCOSE):
        return None

    avg_conf = sum(c for _, c in readings) / len(readings)
    if not any_decimal:
        avg_conf *= NO_DECIMAL_PENALTY
    if avg_conf < READING_CONFIDENCE:
        return None
    return raw, value, avg_conf


# ---------------------------------------------------------------------------
# Synthetic renderer (mirrors the Kotlin test renderer)
# ---------------------------------------------------------------------------
def argb(r, g, b):
    return (0xFF << 24) | (r << 16) | (g << 8) | b


def render(text, w=320, h=150, digit_h=84, bg=200, fg=60, ghost=None,
           label=True, glare=False, top=12, slot_gap=6, shear=0.0, noise=0,
           blur=0):
    px = [argb(int(bg), int(bg), int(bg))] * (w * h)

    def rect(x0, y0, x1, y1, color):
        for y in range(max(0, int(round(y0))), min(h, int(round(y1)))):
            for x in range(max(0, int(round(x0))), min(w, int(round(x1)))):
                yy = y
                xx = x
                if shear:
                    xx = int(round(x + shear * (y - top - digit_h / 2)))
                if 0 <= yy < h and 0 <= xx < w:
                    px[yy * w + xx] = color

    slot_w = int(0.56 * digit_h)
    x = 18.0
    for ch in text:
        if ch == ".":
            x += 0.10 * digit_h
            s = int(0.15 * digit_h)
            rect(x, top + digit_h - s, x + s, top + digit_h, argb(fg, fg, fg))
            x += s + 0.10 * digit_h
            continue
        digit = int(ch)
        color = argb(178, 178, 178) if (ghost is not None and ch == ghost) else argb(fg, fg, fg)
        on = PATTERNS[digit]
        for name in "ABCDEFG":
            if name in on:
                sx, sy, ex, ey = RENDER[name]
                rect(x + sx * slot_w, top + sy * digit_h,
                     x + ex * slot_w, top + ey * digit_h, color)
        x += slot_w + slot_gap

    if label:
        ly = top + digit_h + 14
        for i in range(14):
            rect(30 + i * 11, ly, 36 + i * 11, ly + 16, argb(150, 150, 150))
    if glare:
        rect(w * 0.62, 0, w * 0.95, h * 0.35, argb(255, 255, 255))
    if blur:
        for _ in range(blur):
            nxt = px[:]
            for y in range(1, h - 1):
                for x in range(1, w - 1):
                    idx = y * w + x
                    rs = gs = bs = 0
                    for dy in (-1, 0, 1):
                        for dx in (-1, 0, 1):
                            p = px[(y + dy) * w + x + dx]
                            rs += (p >> 16) & 0xFF
                            gs += (p >> 8) & 0xFF
                            bs += p & 0xFF
                    nxt[idx] = argb(rs // 9, gs // 9, bs // 9)
            px = nxt
    if noise:
        import random as _r
        _r.seed(noise)
        for i in range(len(px)):
            if _r.random() < 0.25:
                d = _r.randint(-noise, noise)
                r = min(255, max(0, ((px[i] >> 16) & 0xFF) + d))
                px[i] = argb(r, r, r)
    return px, w, h


def approx(a, b, eps=0.051):
    return abs(a - b) <= eps


def main():
    failures = []

    def check(name, cond, extra=""):
        status = "PASS" if cond else "FAIL"
        if not cond:
            failures.append(name)
        print(f"[{status}] {name} {extra}")

    for text, val in [("5.7", 5.7), ("6.1", 6.1), ("10.1", 10.1), ("15.2", 15.2),
                      ("25.0", 25.0), ("4.2", 4.2), ("9.9", 9.9), ("11.4", 11.4),
                      ("7.7", 7.7), ("17.3", 17.3)]:
        px, w, h = render(text)
        out = decode(px, w, h)
        ok = out is not None and approx(out[1], val)
        check(f"plain {text}", ok, f"-> {out}")

    px, w, h = render("7.8", label=False)
    out = decode(px, w, h)
    check("no label 7.8", out is not None and approx(out[1], 7.8), f"-> {out}")

    px, w, h = render("6.4", glare=True)
    out = decode(px, w, h)
    check("glare 6.4", out is not None and approx(out[1], 6.4), f"-> {out}")

    px, w, h = render("05.7", ghost="0")
    out = decode(px, w, h)
    check("ghost leading 0", out is not None and approx(out[1], 5.7), f"-> {out}")

    px, w, h = render("85.7", ghost="8")
    out = decode(px, w, h)
    check("faint ghost leading 8 ignored", out is not None and approx(out[1], 5.7), f"-> {out}")

    # A clearly visible ghost 8 makes the reading 85.7 -> refused, not misread.
    px, w, h = render("85.7", ghost="8")
    px2 = []
    for p in px:
        r = (p >> 16) & 0xFF
        if r == 178:
            px2.append(argb(100, 100, 100))  # push ghost into the dark class
        else:
            px2.append(p)
    out = decode(px2, w, h)
    check("visible ghost leading 8 rejected", out is None, f"-> {out}")

    px, w, h = render("8.3", bg=55, fg=215)
    out = decode(px, w, h)
    check("dark bg 8.3", out is not None and approx(out[1], 8.3), f"-> {out}")

    for text, val in [("57", 5.7), ("101", 10.1), ("152", 15.2)]:
        px, w, h = render(text, label=False)
        out = decode(px, w, h)
        ok = out is not None and approx(out[1], val)
        check(f"dropped decimal {text}", ok, f"-> {out}")

    px, w, h = render("25", label=False)
    out = decode(px, w, h)
    check("integer 25 stays 25", out is not None and approx(out[1], 25.0), f"-> {out}")

    px, w, h = render("7", label=False)
    out = decode(px, w, h)
    check("single digit refused", out is None, f"-> {out}")

    px, w, h = render("", label=False)
    out = decode(px, w, h)
    check("blank refused", out is None, f"-> {out}")

    px = [argb(128, 128, 128)] * (320 * 150)
    out = decode(px, 320, 150)
    check("flat gray refused", out is None, f"-> {out}")

    out = decode([argb(200, 200, 200)] * 100, 10, 10)
    check("tiny refused", out is None, f"-> {out}")

    px, w, h = render("35.7", label=False)
    out = decode(px, w, h)
    check("out of range refused", out is None, f"-> {out}")

    # wider digit spacing (meter style)
    px, w, h = render("12.8", slot_gap=20, label=False)
    out = decode(px, w, h)
    check("wide spacing 12.8", out is not None and approx(out[1], 12.8), f"-> {out}")

    # ---- stress: handheld conditions ----
    for text, val in [("5.7", 5.7), ("10.1", 10.1), ("8.3", 8.3)]:
        px, w, h = render(text, shear=0.07)  # ~4 degrees
        out = decode(px, w, h)
        ok = out is not None and approx(out[1], val)
        check(f"shear {text}", ok, f"-> {out}")

    px, w, h = render("6.9", noise=18)
    out = decode(px, w, h)
    check("noise 6.9", out is not None and approx(out[1], 6.9), f"-> {out}")

    px, w, h = render("7.2", blur=1)
    out = decode(px, w, h)
    check("blur 7.2", out is not None and approx(out[1], 7.2), f"-> {out}")

    px, w, h = render("9.4", blur=1, noise=10, shear=0.04)
    out = decode(px, w, h)
    ok = out is None or approx(out[1], 9.4)
    check("blur+noise+shear 9.4 never misreads", ok, f"-> {out}")

    print()
    if failures:
        print("FAILURES:", failures)
        sys.exit(1)
    print("ALL PASS")


if __name__ == "__main__":
    main()
