type RateLimitBucket = {
  count: number;
  resetAt: number;
};

const globalForRateLimit = globalThis as typeof globalThis & {
  __glucoseRateLimitStore?: Map<string, RateLimitBucket>;
};

const store = globalForRateLimit.__glucoseRateLimitStore ?? new Map<string, RateLimitBucket>();

if (!globalForRateLimit.__glucoseRateLimitStore) {
  globalForRateLimit.__glucoseRateLimitStore = store;
}

export type RateLimitResult = {
  allowed: boolean;
  limit: number;
  remaining: number;
  resetAt: number;
};

export function consumeRateLimit(key: string, limit: number, windowMs: number): RateLimitResult {
  const now = Date.now();
  const bucket = store.get(key);

  if (!bucket || bucket.resetAt <= now) {
    const resetAt = now + windowMs;
    store.set(key, { count: 1, resetAt });
    return { allowed: true, limit, remaining: Math.max(0, limit - 1), resetAt };
  }

  const nextCount = bucket.count + 1;
  bucket.count = nextCount;

  const allowed = nextCount <= limit;
  return {
    allowed,
    limit,
    remaining: Math.max(0, limit - nextCount),
    resetAt: bucket.resetAt,
  };
}

export function getClientIp(req: Request): string {
  const xForwardedFor = req.headers.get("x-forwarded-for");
  if (xForwardedFor) {
    const first = xForwardedFor.split(",")[0]?.trim();
    if (first) return first;
  }

  const realIp = req.headers.get("x-real-ip");
  if (realIp) return realIp;

  return "unknown";
}

export function isLikelySameOrigin(req: Request): boolean {
  const origin = req.headers.get("origin");
  const host = req.headers.get("host");

  if (!origin || !host) return true;

  try {
    const url = new URL(origin);
    return url.host === host;
  } catch {
    return false;
  }
}

export function isJsonContentType(req: Request): boolean {
  const contentType = req.headers.get("content-type") ?? "";
  return contentType.toLowerCase().includes("application/json");
}

export async function parseJsonWithSizeLimit<T>(req: Request, maxBytes: number): Promise<T> {
  const raw = await req.text();
  const bytes = new TextEncoder().encode(raw).byteLength;

  if (bytes > maxBytes) {
    throw new Error("PAYLOAD_TOO_LARGE");
  }

  return JSON.parse(raw) as T;
}

export function normalizeOptionalText(input: unknown, maxLength: number): string | null {
  if (typeof input !== "string") return null;
  const normalized = input.replace(/\s+/g, " ").trim();
  if (!normalized) return null;
  return normalized.slice(0, maxLength);
}

export function sanitizeFiniteNumber(input: unknown): number | null {
  if (typeof input !== "number") return null;
  return Number.isFinite(input) ? input : null;
}
