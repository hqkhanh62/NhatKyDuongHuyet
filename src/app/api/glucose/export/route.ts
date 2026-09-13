import { db } from "@/db";
import { glucoseEntries } from "@/db/schema";
import { assertRole, requireSessionUser } from "@/lib/auth";
import { writeAuditLog } from "@/lib/audit";
import { encryptPayload, toCsv } from "@/lib/export";
import { getClientIp, isJsonContentType, isLikelySameOrigin, parseJsonWithSizeLimit } from "@/lib/security";
import { desc, eq } from "drizzle-orm";
import { NextResponse } from "next/server";

type ExportBody = {
  format?: "json" | "csv";
  passphrase?: string;
  userId?: number;
};

export async function POST(req: Request) {
  const ip = getClientIp(req);
  const userAgent = req.headers.get("user-agent");

  if (!isLikelySameOrigin(req)) {
    return NextResponse.json({ error: "Origin không hợp lệ." }, { status: 403 });
  }

  if (!isJsonContentType(req)) {
    return NextResponse.json({ error: "Content-Type phải là application/json." }, { status: 415 });
  }

  try {
    const actor = await requireSessionUser();
    const body = await parseJsonWithSizeLimit<ExportBody>(req, 8 * 1024);

    const format = body.format === "csv" ? "csv" : "json";
    const passphrase = String(body.passphrase ?? "");

    if (passphrase.length < 12) {
      return NextResponse.json({ error: "Passphrase export phải tối thiểu 12 ký tự." }, { status: 400 });
    }

    let targetUserId = actor.id;
    if (typeof body.userId === "number" && Number.isInteger(body.userId) && body.userId > 0) {
      if (body.userId !== actor.id) {
        assertRole(actor, ["admin"]);
      }
      targetUserId = body.userId;
    }

    const rows = await db
      .select()
      .from(glucoseEntries)
      .where(eq(glucoseEntries.userId, targetUserId))
      .orderBy(desc(glucoseEntries.measuredAt));

    const payload =
      format === "csv"
        ? toCsv(
            rows.map((r) => ({
              ...r,
              measuredAt: new Date(r.measuredAt),
              createdAt: new Date(r.createdAt),
            })),
          )
        : JSON.stringify(
            {
              exportedAt: new Date().toISOString(),
              exportedBy: actor.id,
              targetUserId,
              unit: "mg/dL",
              entries: rows,
            },
            null,
            2,
          );

    const encrypted = encryptPayload(payload, passphrase);

    await writeAuditLog({
      userId: actor.id,
      action: "glucose.export",
      resource: "glucose_entries",
      success: true,
      metadata: {
        format,
        targetUserId,
        count: rows.length,
        encrypted: true,
      },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json(
      {
        fileName: `glucose-export-user-${targetUserId}-${new Date().toISOString()}.${format}.enc.json`,
        package: encrypted,
      },
      {
        headers: {
          "Cache-Control": "no-store",
        },
      },
    );
  } catch (error) {
    const reason = error instanceof Error ? error.message : "UNKNOWN";

    if (reason === "PAYLOAD_TOO_LARGE") {
      return NextResponse.json({ error: "Payload quá lớn." }, { status: 413 });
    }

    if (reason === "UNAUTHORIZED") {
      await writeAuditLog({
        userId: null,
        action: "glucose.export",
        resource: "glucose_entries",
        success: false,
        metadata: { reason },
        ipAddress: ip,
        userAgent,
      });
      return NextResponse.json({ error: "Bạn chưa đăng nhập." }, { status: 401 });
    }

    if (reason === "FORBIDDEN") {
      await writeAuditLog({
        userId: null,
        action: "glucose.export",
        resource: "glucose_entries",
        success: false,
        metadata: { reason },
        ipAddress: ip,
        userAgent,
      });
      return NextResponse.json({ error: "Bạn không có quyền export dữ liệu này." }, { status: 403 });
    }

    await writeAuditLog({
      userId: null,
      action: "glucose.export",
      resource: "glucose_entries",
      success: false,
      metadata: { reason },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json({ error: "Không thể export dữ liệu." }, { status: 500 });
  }
}
