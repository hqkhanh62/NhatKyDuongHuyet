"""Faithful Python replication of PixelGlucoseReader (Kotlin) + synthetic 7-seg renderer."""
import numpy as np

SEGMENT_ON_THRESHOLD = 0.28
DECIMAL_POINT_THRESHOLD = 0.25
PIXEL_DIGIT_CONFIDENCE = 0.70
PIXEL_READING_CONFIDENCE = 0.78
MIN_G, MAX_G = 2.0, 30.0

SEGS = "ABCDEFG"
PATTERNS = {
    0: set("ABCDEF"), 1: set("BC"), 2: set("ABGED"), 3: set("ABCDG"),
    4: set("FGBC"), 5: set("AFGCD"), 6: set("AFGECD"), 7: set("ABC"),
    8: set("ABCDEFG"), 9: set("ABCDFG"),
}
# rects as in Kotlin readDigit()
SEG_RECT = {
    "A": (0.20, 0.08, 0.80, 0.18), "B": (0.82, 0.15, 0.95, 0.45),
    "C": (0.82, 0.55, 0.95, 0.85), "D": (0.20, 0.82, 0.80, 0.92),
    "E": (0.05, 0.55, 0.18, 0.85), "F": (0.05, 0.15, 0.18, 0.45),
    "G": (0.20, 0.45, 0.80, 0.55),
}
CELLS = [(0.05,0.15,0.32,0.85), (0.35,0.15,0.62,0.85), (0.68,0.15,0.95,0.85)]

def dark_ratio(lum, rect):
    h, w = lum.shape
    l = min(max(int(w*rect[0]), 0), w-1); t = min(max(int(h*rect[1]), 0), h-1)
    r = min(max(int(w*rect[2]), l+1), w);  b = min(max(int(h*rect[3]), t+1), h)
    patch = lum[t:b, l:r]
    n = patch.size
    if n <= 0: return 0.0
    thr = min(max(patch.mean()*0.85, 40.0), 180.0)
    return float((patch < thr).sum())/n

def read_digit(lum, cell):
    active = set()
    cw, ch = cell[2]-cell[0], cell[3]-cell[1]
    for s, r in SEG_RECT.items():
        ab = (cell[0]+r[0]*cw, cell[1]+r[1]*ch, cell[0]+r[2]*cw, cell[1]+r[3]*ch)
        if dark_ratio(lum, ab) >= SEGMENT_ON_THRESHOLD:
            active.add(s)
    if not active: return None
    best, bscore = None, None
    for d, on in PATTERNS.items():
        sc = len(on & active)*1.0 - len(on-active)*0.8 - len(active-on)*0.6
        if bscore is None or sc > bscore: best, bscore = d, sc
    on = PATTERNS[best]
    conf = len(on & active)/len(on | active)
    return (best, conf, "".join(sorted(active))) if conf >= PIXEL_DIGIT_CONFIDENCE else (None, conf, "".join(sorted(active)))

def process_display(lum):
    readings, dbg = [], []
    for c in CELLS:
        d = read_digit(lum, c)
        dbg.append(d)
        if d and d[0] is not None: readings.append(d)
    dec = dark_ratio(lum, (0.62,0.75,0.68,0.90)) > DECIMAL_POINT_THRESHOLD
    if not readings: return None, dbg, dec
    txt = ""
    for i, (d, conf, _) in enumerate(readings):
        if dec and i == len(readings)-1: txt += "."
        txt += str(d)
    try: val = float(txt)
    except ValueError: return None, dbg, dec
    if not (MIN_G <= val <= MAX_G): return ("OUT_OF_RANGE", txt), dbg, dec
    conf = sum(c for _, c, _ in readings)/len(readings)
    return ((txt, val, round(conf,2)) if conf >= PIXEL_READING_CONFIDENCE else ("LOW_CONF", txt, round(conf,2))), dbg, dec

# ---------- synthetic renderer ----------
DRAW = {"A":(0.18,0.05,0.82,0.16),"B":(0.84,0.11,0.97,0.48),"C":(0.84,0.52,0.97,0.89),
        "D":(0.18,0.84,0.82,0.95),"E":(0.03,0.52,0.16,0.89),"F":(0.03,0.11,0.16,0.48),
        "G":(0.18,0.45,0.82,0.56)}

def render(text, W=900, H=600, digits_w=0.9, digits_h=0.7, cx=0.5, cy=0.5,
           fg=20, bg=245):
    """text like '5.7'. digits_w/h = fraction of image occupied by digit block."""
    img = np.full((H, W), float(bg))
    chars = [c for c in text if c.isdigit()]
    n = len(chars)
    block_w, block_h = W*digits_w, H*digits_h
    x0, y0 = W*cx - block_w/2, H*cy - block_h/2
    dw = block_w/n
    for i, ch in enumerate(chars):
        dx0 = x0 + i*dw
        for s in PATTERNS[int(ch)]:
            r = DRAW[s]
            l = int(dx0 + r[0]*dw*0.85); rr = int(dx0 + r[2]*dw*0.85)
            t = int(y0 + r[1]*block_h);  b = int(y0 + r[3]*block_h)
            img[max(t,0):b, max(l,0):rr] = fg
    if "." in text:
        i = text.index(".") - text[:text.index(".")].count(".")
        ndig = len([c for c in text[:text.index(".")] if c.isdigit()])
        px = int(x0 + ndig*dw - dw*0.10); py = int(y0 + block_h*0.88)
        img[py:py+int(block_h*0.09), px:px+int(dw*0.09)] = fg
    return img

def run(name, **kw):
    text = kw.pop("text")
    lum = render(text, **kw)
    res, dbg, dec = process_display(lum)
    segs = " | ".join(f"cell{i}:{d[2] if d else '-'}->{d[0] if d else None}" for i, d in enumerate(dbg))
    print(f"{name:52s} display={text:6s} -> {str(res):28s} dp={str(dec):5s}  {segs}")

print("=== A. Digits perfectly filling the ROI, max contrast (ideal lab case) ===")
for t in ["5.7","6.1","12.3","9.4","8.8"]:
    run("ideal fill", text=t, digits_w=0.9, digits_h=0.7, fg=15, bg=250)

print("\n=== B. Same, but real-photo contrast (LCD grey digits on grey bg) ===")
for fgv, bgv in [(60,200),(90,190),(110,205),(130,215)]:
    run(f"contrast fg={fgv} bg={bgv}", text="5.7", digits_w=0.9, digits_h=0.7, fg=fgv, bg=bgv)

print("\n=== C. Realistic framing: user frames the whole meter LCD, digits smaller/off-centre ===")
for dw_, dh_, cx_, cy_ in [(0.60,0.50,0.50,0.50),(0.70,0.55,0.45,0.45),(0.50,0.45,0.55,0.55),(0.80,0.60,0.50,0.42)]:
    run(f"framing w={dw_} h={dh_} c=({cx_},{cy_})", text="5.7", digits_w=dw_, digits_h=dh_, cx=cx_, cy=cy_, fg=15, bg=250)

print("\n=== D. Inverted display (light digits on dark background) ===")
run("inverted", text="5.7", digits_w=0.9, digits_h=0.7, fg=240, bg=20)
run("inverted", text="12.3", digits_w=0.9, digits_h=0.7, fg=240, bg=20)

print("\n=== E. Two-digit value that only fills 2 of the 3 assumed cells ===")
for t in ["5.7","6.1"]:
    run("2-digit block in left 2/3", text=t, digits_w=0.62, digits_h=0.7, cx=0.35, fg=15, bg=250)

print("\n\n=== F. Sweep: how often does the pixel reader deliver a WRONG value? ===")
vals = [round(v/10,1) for v in range(30, 200, 3)]  # 3.0 .. 19.9 mmol/L
conds = [
 ("ideal contrast, digits fill frame", dict(digits_w=0.9,digits_h=0.7,fg=15,bg=250)),
 ("ideal contrast, digits 70% of frame", dict(digits_w=0.7,digits_h=0.6,fg=15,bg=250)),
 ("photo contrast (fg110/bg205)",       dict(digits_w=0.9,digits_h=0.7,fg=110,bg=205)),
 ("inverted display",                    dict(digits_w=0.9,digits_h=0.7,fg=240,bg=20)),
]
for name, kw in conds:
    ok = wrong = none = oor = 0
    examples = []
    for v in vals:
        lum = render(str(v), **kw)
        res, _, _ = process_display(lum)
        if res is None: none += 1
        elif isinstance(res, tuple) and res[0] in ("OUT_OF_RANGE","LOW_CONF"): oor += 1
        else:
            got = res[1]
            if abs(got - v) < 0.05: ok += 1
            else:
                wrong += 1
                if len(examples) < 5: examples.append(f"{v}->{got}")
    n = len(vals)
    print(f"{name:38s} correct={ok*100//n:3d}%  WRONG_DELIVERED={wrong*100//n:3d}%  no-result={none*100//n:3d}%  rejected={oor*100//n:3d}%   e.g. {', '.join(examples)}")

print("\n=== G. Isolated proof of the adaptive-threshold bug (uniform LIT segment patch) ===")
for lumv in [10, 30, 45, 60, 90, 120, 150, 175, 200, 240]:
    patch = np.full((40, 40), float(lumv))
    print(f"  segment patch luminance={lumv:3d} -> darkPixelRatio={dark_ratio(patch,(0,0,1,1)):.2f} -> {'ON ' if dark_ratio(patch,(0,0,1,1))>=SEGMENT_ON_THRESHOLD else 'OFF'}")
