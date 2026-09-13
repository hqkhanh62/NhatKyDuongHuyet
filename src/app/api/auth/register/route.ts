import { db } from "@/db";
import { users } from "@/db/schema";
import { createUserSession, hashPassword, normalizeEmail } from "@/lib/auth";
import { writeAuditLog } from "@/lib/audit";
import { getClientIp, isJsonContentType, isLikelySameOrigin, normalizeOptionalText, parseJsonWithSizeLimit } from "@/lib/security";
import { eq } from "drizzle-orm";
import { NextResponse } from "next/server";

type RegisterBody = {
  email?: string;
  fullName?: string;
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
    const body = await parseJsonWithSizeLimit<RegisterBody>(req, 8 * 1024);

    const email = normalizeEmail(String(body.email ?? ""));
    const fullName = normalizeOptionalText(body.fullName, 80);
    const password = String(body.password ?? "");

    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
      return NextResponse.json({ error: "Email không hợp lệ." }, { status: 400 });
    }

    if (!fullName || fullName.length < 2) {
      return NextResponse.json({ error: "Họ tên phải từ 2 ký tự." }, { status: 400 });
    }

    if (password.length < 10) {
      return NextResponse.json({ error: "Mật khẩu phải tối thiểu 10 ký tự." }, { status: 400 });
    }

    const existing = await db.select({ id: users.id }).from(users).where(eq(users.email, email)).limit(1);
    if (existing[0]) {
      await writeAuditLog({
        userId: null,
        action: "auth.register",
        resource: "users",
        success: false,
        metadata: { reason: "email_exists", email },
        ipAddress: ip,
        userAgent,
      });

      return NextResponse.json({ error: "Email đã được sử dụng." }, { status: 409 });
    }

    const passwordHash = hashPassword(password);

    const existingCount = await db.select({ id: users.id }).from(users).limit(1);
    const bootstrapRole = existingCount[0] ? "user" : "admin";

    const [created] = await db
      .insert(users)
      .values({
        email,
        fullName,
        passwordHash,
        role: bootstrapRole,
      })
      .returning();

    await createUserSession({ userId: created.id, ipAddress: ip, userAgent });

    await writeAuditLog({
      userId: created.id,
      action: "auth.register",
      resource: "users",
      resourceId: String(created.id),
      success: true,
      metadata: { email },
      ipAddress: ip,
      userAgent,
    });

    return NextResponse.json({
      user: {
        id: created.id,
        email: created.email,
        fullName: created.fullName,
        role: created.role,
      },
    });
  } catch (error) {
    if (error instanceof Error && error.message === "PAYLOAD_TOO_LARGE") {
      return NextResponse.json({ error: "Payload quá lớn." }, { status: 413 });
    }

    return NextResponse.json({ error: "Không thể đăng ký tài khoản." }, { status: 500 });
  }
}
