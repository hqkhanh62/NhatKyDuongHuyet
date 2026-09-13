import { clearUserSession, getSessionUser } from "@/lib/auth";
import { writeAuditLog } from "@/lib/audit";
import { getClientIp, isLikelySameOrigin } from "@/lib/security";
import { NextResponse } from "next/server";

export async function POST(req: Request) {
  const ip = getClientIp(req);
  const userAgent = req.headers.get("user-agent");

  if (!isLikelySameOrigin(req)) {
    return NextResponse.json({ error: "Origin không hợp lệ." }, { status: 403 });
  }

  const user = await getSessionUser();
  await clearUserSession();

  await writeAuditLog({
    userId: user?.id ?? null,
    action: "auth.logout",
    resource: "users",
    resourceId: user ? String(user.id) : null,
    success: true,
    metadata: null,
    ipAddress: ip,
    userAgent,
  });

  return NextResponse.json({ ok: true });
}
