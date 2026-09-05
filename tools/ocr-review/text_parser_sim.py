import re
MIN_G, MAX_G, MG = 2.0, 30.0, 18.0
number_re = re.compile(r"(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])")
dt_re = re.compile(r"\b[0-9]{1,4}[/\-][0-9]{1,2}(?:[/\-][0-9]{1,4})?\b|\b(?:[01]?\d|2[0-3]):[0-5]\d\b")

def normalize(text):
    t = text.replace('\u00A0',' ').replace('٫','.').replace('，','.').replace(',','.')
    t = re.sub(r"(?<=\d)\s*[.]\s*(?=\d)", ".", t)
    t = re.sub(r"(?<!\d)(\d{1,2})\s+(\d)(?=\s*(?:mmol|mg(?:/\s*dl)?|$))", r"\1.\2", t, flags=re.I|re.M)
    return re.sub(r"[ \t]+", " ", t).strip()

def norm_tok(tok):
    for a,b in [('O','0'),('o','0'),('Q','0'),('q','0'),('I','1'),('i','1'),('L','1'),('l','1'),('|','1')]:
        tok = tok.replace(a,b)
    return tok

def extract(text):
    if not text.strip(): return None
    cands, pos = [], 0
    for line in normalize(text).split("\n"):
        lc = line.lower()
        mmol, mg = "mmol" in lc, "mg" in lc
        label = any(k in lc for k in ("glucose","sugar","result","value"))
        if dt_re.search(line) and not (label or mmol or mg):
            pos += len(line)+1; continue
        for m in number_re.finditer(line):
            try: raw = float(norm_tok(m.group(1)))
            except ValueError: continue
            if mg: v = raw/MG
            elif raw > 20 and not mmol: continue
            elif MIN_G <= raw <= MAX_G: v = raw
            else: continue
            if not (MIN_G <= v <= MAX_G): continue
            s = (100 if mmol else 0)+(90 if mg else 0)+(25 if raw%1 else 0)+(10 if 3<=v<=20 else 0)+(20 if label else 0)
            cands.append((s, pos+m.start(), v))
        pos += len(line)+1
    if not cands: return None
    cands.sort(key=lambda c:(-c[0], c[1]))
    return cands[0][2]

cases = [
 ("5.7 mmol/L", 5.7, "normal"),
 ("5 7 mmol/L", 5.7, "split digits"),
 ("10 24", None, "TIME 10:24 with colon lost by OCR"),
 ("1024", None, "time 10:24 without separator"),
 ("08 32\n5.7", 5.7, "time row 08:32 colon lost + real value"),
 ("HI", None, "meter shows HI (>33.3 mmol)"),
 ("Lo", None, "meter shows LO"),
 ("E-3", None, "meter error code"),
 ("AVG 7.2", None, "14-day average screen (not a fresh reading)"),
 ("MEM 12 5.7", 5.7, "memory screen"),
 ("126 mg/dL", 7.0, "mg/dL conversion"),
 ("126", None, "mg/dL meter, unit not inside crop"),
 ("5.7 mmol/L\n88 mg/dL", 5.7, "dual unit display"),
 ("6.1 12:45", 6.1, "value + time on the SAME OCR line"),
 ("20-08 6.2", 6.2, "date + value same line"),
 ("57 mmol/L", None, "decimal point lost, unit visible"),
 ("l2.3 mmol/L", 12.3, "l->1 substitution"),
 ("8", 8.0, "single integer digit, no unit, no decimal"),
 ("28", None, "28 without unit"),
 ("28 mmol/L", 28.0, "28 with unit"),
 ("3", 3.0, "'3' from a stray label e.g. strip code"),
 ("Ketone 0.6\n5.7 mmol/L", 5.7, "ketone line present"),
 ("%5.7", 5.7, "HbA1c percent sign"),
 ("HbA1c 6.5 %", 6.5, "HbA1c 6.5% misread as glucose"),
]
print(f"{'input':30s} {'expected':>9s} {'actual':>9s}  note")
print("-"*95)
for txt, exp, note in cases:
    got = extract(txt)
    flag = "OK " if (got is None and exp is None) or (got is not None and exp is not None and abs(got-exp)<0.05) else "FAIL"
    print(f"{flag} {repr(txt)[:28]:28s} {str(exp):>9s} {str(round(got,2) if got else got):>9s}  {note}")
