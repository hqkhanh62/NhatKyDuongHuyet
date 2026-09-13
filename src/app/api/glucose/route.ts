import { db } from "@/db";
import { glucoseEntries } from "@/db/schema";
import { requireSessionUser } from "@/lib/auth";
import { writeAuditLog } from "@/lib/audit";
import { inferAndNormalizeGlucose, isValidMgDl } from "@/lib/glucose";
import {
  consumeRateLimit,
  getClientIp,
  isJsonContentType,
  isLikelySameOrigin,
  normalizeOptionalText,
  parseJsonWithSizeLimit,
  sanitizeFiniteNumber,
} from "@/lib/security";
import { and, desc, eq } from "drizzle-orm";
import { NextResponse } from "next/server";

const JSON_PAYLOAD_LIMIT_BYTES = 16 * 1024;
const MAX_NOTE_LENGTH = 280;
const MAX_OCR_RAW_LENGTH = 1000;

type CreateGlucoseEntryBody = {
  input?: string;
  source?: "manual" | "camera_ocr";
  note?: string;
  measuredAt?: string;
  ocrRawText?: string;
  ocrConfidence?: number;
};

export async function GET(req: Request) {
  const ip = getClientIp(req);
  const userAgent = req.headers.get("user-agent");
  const rate = consumeRateLimit(`glucose:get:${ip}`, 90, 60_000);

  if (!rate.allowed) {
    return NextResponse.json(
      { error: "Quá nhiều yêu cầu. Vui lòng thử lại sau." },
      {
        status: 429,
        headers: {
          "Cache-Control": "no-store",
          "X-RateLimit-Limit": String(rate.limit),
          "X-RateLimit-Remaining": String(rate.remaining),
          "X-RateLimit-Reset": String(Math.floor(rate.resetAt / 1000)),
        },
      },
    );
  }

  try {
    const user = await requireSessionUser();

    const rows = await db
      .select()
      .from(glucoseEntries)
      .where(eq(glucoseEntries.userId, user.id))
      .orderBy(desc(glucoseEntries.measuredAt))
      .limit(50);

    await writeAuditLog({
      userId: user.id,
      action: "glucose.read",
      resource: "glucose_entries",
      success: true,
      metadata: { count: rows.length },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json(
      { entries: rows },
      {
        headers: {
          "Cache-Control": "no-store",
          "X-RateLimit-Limit": String(rate.limit),
          "X-RateLimit-Remaining": String(rate.remaining),
          "X-RateLimit-Reset": String(Math.floor(rate.resetAt / 1000)),
        },
      },
    );
  } catch (error) {
    const reason = error instanceof Error ? error.message : "UNKNOWN";
    if (reason === "UNAUTHORIZED") {
      return NextResponse.json({ error: "Bạn chưa đăng nhập." }, { status: 401 });
    }
    return NextResponse.json({ error: "Không thể tải dữ liệu." }, { status: 500 });
  }
}

export async function POST(req: Request) {
  const ip = getClientIp(req);
  const userAgent = req.headers.get("user-agent");
  const rate = consumeRateLimit(`glucose:post:${ip}`, 40, 60_000);

  if (!rate.allowed) {
    return NextResponse.json(
      { error: "Bạn đang thao tác quá nhanh. Vui lòng thử lại sau." },
      {
        status: 429,
        headers: {
          "Cache-Control": "no-store",
          "X-RateLimit-Limit": String(rate.limit),
          "X-RateLimit-Remaining": String(rate.remaining),
          "X-RateLimit-Reset": String(Math.floor(rate.resetAt / 1000)),
        },
      },
    );
  }

  if (!isLikelySameOrigin(req)) {
    return NextResponse.json({ error: "Origin không hợp lệ." }, { status: 403 });
  }

  if (!isJsonContentType(req)) {
    return NextResponse.json({ error: "Content-Type phải là application/json." }, { status: 415 });
  }

  try {
    const user = await requireSessionUser();
    const body = await parseJsonWithSizeLimit<CreateGlucoseEntryBody>(req, JSON_PAYLOAD_LIMIT_BYTES);
    const source = body.source === "camera_ocr" ? "camera_ocr" : "manual";

    const input = normalizeOptionalText(body.input, 64);
    const parsed = inferAndNormalizeGlucose(String(input ?? ""));

    if (!parsed) {
      return NextResponse.json(
        { error: "Không nhận diện được chỉ số đường huyết hợp lệ." },
        { status: 400 },
      );
    }

    if (!isValidMgDl(parsed.valueMgDl)) {
      return NextResponse.json({ error: "Giá trị ngoài khoảng cho phép (20-600 mg/dL)." }, { status: 400 });
    }

    const measuredAt = body.measuredAt ? new Date(body.measuredAt) : new Date();
    if (Number.isNaN(measuredAt.getTime())) {
      return NextResponse.json({ error: "Thời gian đo không hợp lệ." }, { status: 400 });
    }

    const now = Date.now();
    const measuredAtTime = measuredAt.getTime();
    const maxFuture = now + 5 * 60 * 1000;
    const maxPast = now - 10 * 365 * 24 * 60 * 60 * 1000;
    if (measuredAtTime > maxFuture || measuredAtTime < maxPast) {
      return NextResponse.json({ error: "Thời gian đo vượt ngoài phạm vi hợp lệ." }, { status: 400 });
    }

    const note = normalizeOptionalText(body.note, MAX_NOTE_LENGTH);
    const ocrRawText = normalizeOptionalText(body.ocrRawText, MAX_OCR_RAW_LENGTH);

    let ocrConfidence = sanitizeFiniteNumber(body.ocrConfidence);
    if (ocrConfidence !== null) {
      ocrConfidence = Math.max(0, Math.min(100, ocrConfidence));
    }

    const [inserted] = await db
      .insert(glucoseEntries)
      .values({
        userId: user.id,
        valueMgDl: parsed.valueMgDl,
        originalValue: parsed.value,
        originalUnit: parsed.unit,
        source,
        note,
        measuredAt,
        ocrRawText,
        ocrConfidence,
      })
      .returning();

    await writeAuditLog({
      userId: user.id,
      action: "glucose.create",
      resource: "glucose_entries",
      resourceId: String(inserted.id),
      success: true,
      metadata: { source: inserted.source },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json(
      { entry: inserted },
      {
        status: 201,
        headers: {
          "Cache-Control": "no-store",
          "X-RateLimit-Limit": String(rate.limit),
          "X-RateLimit-Remaining": String(rate.remaining),
          "X-RateLimit-Reset": String(Math.floor(rate.resetAt / 1000)),
        },
      },
    );
  } catch (error) {
    const reason = error instanceof Error ? error.message : "UNKNOWN";

    if (reason === "PAYLOAD_TOO_LARGE") {
      return NextResponse.json({ error: "Payload quá lớn." }, { status: 413 });
    }

    if (reason === "UNAUTHORIZED") {
      return NextResponse.json({ error: "Bạn chưa đăng nhập." }, { status: 401 });
    }

    return NextResponse.json({ error: "Có lỗi khi lưu dữ liệu." }, { status: 500 });
  }
}
