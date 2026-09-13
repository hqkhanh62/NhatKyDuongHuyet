import { db } from "@/db";
import { users } from "@/db/schema";
import { assertRole, requireSessionUser } from "@/lib/auth";
import { writeAuditLog } from "@/lib/audit";
import { getClientIp } from "@/lib/security";
import { desc } from "drizzle-orm";
import { NextResponse } from "next/server";

export async function GET(req: Request) {
  const ip = getClientIp(req);
  const userAgent = req.headers.get("user-agent");

  try {
    const actor = await requireSessionUser();
    assertRole(actor, ["admin"]);

    const rows = await db
      .select({
        id: users.id,
        email: users.email,
        fullName: users.fullName,
        role: users.role,
        createdAt: users.createdAt,
        updatedAt: users.updatedAt,
      })
      .from(users)
      .orderBy(desc(users.createdAt))
      .limit(200);

    await writeAuditLog({
      userId: actor.id,
      action: "admin.users.read",
      resource: "users",
      success: true,
      metadata: { count: rows.length },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json({ users: rows }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    const reason = error instanceof Error ? error.message : "UNKNOWN";

    await writeAuditLog({
      userId: null,
      action: "admin.users.read",
      resource: "users",
      success: false,
      metadata: { reason },
      ipAddress: ip,
      userAgent,
    });

    if (reason === "UNAUTHORIZED") {
      return NextResponse.json({ error: "Bạn chưa đăng nhập." }, { status: 401 });
    }

    if (reason === "FORBIDDEN") {
      return NextResponse.json({ error: "Bạn không có quyền truy cập." }, { status: 403 });
    }

    return NextResponse.json({ error: "Không thể lấy danh sách người dùng." }, { status: 500 });
  }
}
