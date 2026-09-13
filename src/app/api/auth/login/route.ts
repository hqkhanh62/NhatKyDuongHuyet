import { db } from "@/db";
import { users } from "@/db/schema";
import { createUserSession, normalizeEmail, verifyPassword } from "@/lib/auth";
import { writeAuditLog } from "@/lib/audit";
import { getClientIp, isJsonContentType, isLikelySameOrigin, parseJsonWithSizeLimit } from "@/lib/security";
import { eq } from "drizzle-orm";
import { NextResponse } from "next/server";

type LoginBody = {
  email?: string;
  password?: string;
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
    const body = await parseJsonWithSizeLimit<LoginBody>(req, 8 * 1024);
    const email = normalizeEmail(String(body.email ?? ""));
    const password = String(body.password ?? "");

    if (!email || !password) {
      return NextResponse.json({ error: "Email hoặc mật khẩu không hợp lệ." }, { status: 400 });
    }

    const found = await db.select().from(users).where(eq(users.email, email)).limit(1);
    const user = found[0];

    if (!user || !verifyPassword(password, user.passwordHash)) {
      await writeAuditLog({
        userId: user?.id ?? null,
        action: "auth.login",
        resource: "users",
        success: false,
        metadata: { email, reason: "invalid_credentials" },
        ipAddress: ip,
        userAgent,
      });
      return NextResponse.json({ error: "Sai email hoặc mật khẩu." }, { status: 401 });
    }

    await createUserSession({ userId: user.id, ipAddress: ip, userAgent });

    await writeAuditLog({
      userId: user.id,
      action: "auth.login",
      resource: "users",
      resourceId: String(user.id),
      success: true,
      metadata: { email },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json({
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
      },
    });
  } catch (error) {
    if (error instanceof Error && error.message === "PAYLOAD_TOO_LARGE") {
      return NextResponse.json({ error: "Payload quá lớn." }, { status: 413 });
    }

    return NextResponse.json({ error: "Không thể đăng nhập." }, { status: 500 });
  }
}
