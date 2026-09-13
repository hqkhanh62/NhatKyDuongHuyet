export type GlucoseUnit = "mg/dL" | "mmol/L";

export type ParsedGlucose = {
  value: number;
  unit: GlucoseUnit;
  valueMgDl: number;
  score: number;
  matched: string;
};

const MGDL_MIN = 20;
const MGDL_MAX = 600;
const MMOLL_MIN = 1.1;
const MMOLL_MAX = 33.3;

export function mmolToMgDl(valueMmol: number): number {
  return Math.round(valueMmol * 18);
}

export function mgDlToMmol(valueMgDl: number): number {
  return Number((valueMgDl / 18).toFixed(1));
}

export function isValidMgDl(valueMgDl: number): boolean {
  return Number.isFinite(valueMgDl) && valueMgDl >= MGDL_MIN && valueMgDl <= MGDL_MAX;
}

export function inferAndNormalizeGlucose(input: string): ParsedGlucose | null {
  const parsed = extractBestCandidate(input);
  if (!parsed) return null;

  const valueMgDl = parsed.unit === "mmol/L" ? mmolToMgDl(parsed.value) : Math.round(parsed.value);
  if (!isValidMgDl(valueMgDl)) {
    return null;
  }

  return {
    ...parsed,
    valueMgDl,
  };
}

export function extractBestCandidate(rawText: string): ParsedGlucose | null {
  const text = rawText.replace(/\s+/g, " ").trim();
  if (!text) return null;

  const hasMmolHint = /mmol|mmo1|mm0l|mm0\/l|mmo\/l|mmol\/l/i.test(text);
  const hasMgHint = /mg\/?dl|mgdl/i.test(text);

  const matches = [...text.matchAll(/\d{1,3}(?:[\.,]\d{1,2})?/g)].map((m) => m[0]);
  if (matches.length === 0) return null;

  const candidates: ParsedGlucose[] = [];

  for (const matched of matches) {
    const normalized = matched.replace(",", ".");
    const numericValue = Number(normalized);
    if (!Number.isFinite(numericValue)) continue;

    const hasDecimal = normalized.includes(".");

    if (hasMmolHint || (hasDecimal && numericValue >= MMOLL_MIN && numericValue <= MMOLL_MAX)) {
      if (numericValue >= MMOLL_MIN && numericValue <= MMOLL_MAX) {
        candidates.push({
          value: Number(numericValue.toFixed(1)),
          unit: "mmol/L",
          valueMgDl: mmolToMgDl(numericValue),
          score: scoreCandidate(numericValue, "mmol/L", hasMmolHint, hasMgHint, hasDecimal),
          matched,
        });
      }
    }

    if (hasMgHint || (!hasDecimal && numericValue >= MGDL_MIN && numericValue <= MGDL_MAX)) {
      if (numericValue >= MGDL_MIN && numericValue <= MGDL_MAX) {
        candidates.push({
          value: Math.round(numericValue),
          unit: "mg/dL",
          valueMgDl: Math.round(numericValue),
          score: scoreCandidate(numericValue, "mg/dL", hasMmolHint, hasMgHint, hasDecimal),
          matched,
        });
      }
    }
  }

  if (candidates.length === 0) {
    return null;
  }

  candidates.sort((a, b) => b.score - a.score);
  return candidates[0] ?? null;
}

function scoreCandidate(
  value: number,
  unit: GlucoseUnit,
  hasMmolHint: boolean,
  hasMgHint: boolean,
  hasDecimal: boolean,
): number {
  let score = 0;

  if (unit === "mmol/L") {
    if (hasMmolHint) score += 3;
    if (hasDecimal) score += 2;
    if (value >= 3.5 && value <= 10.5) score += 3;
    else if (value >= MMOLL_MIN && value <= MMOLL_MAX) score += 1;
  }

  if (unit === "mg/dL") {
    if (hasMgHint) score += 3;
    if (!hasDecimal) score += 2;
    if (value >= 70 && value <= 220) score += 3;
    else if (value >= MGDL_MIN && value <= MGDL_MAX) score += 1;
  }

  if (hasMmolHint && unit === "mg/dL") score -= 2;
  if (hasMgHint && unit === "mmol/L") score -= 2;

  return score;
}
