import { db } from "@/db";
import { auditLogs } from "@/db/schema";

export type AuditAction =
  | "auth.register"
  | "auth.login"
  | "auth.logout"
  | "glucose.read"
  | "glucose.create"
  | "glucose.export"
  | "admin.users.read";

export async function writeAuditLog(params: {
  userId: number | null;
  action: AuditAction;
  resource: string;
  resourceId?: string | null;
  success: boolean;
  metadata?: Record<string, unknown> | null;
  ipAddress?: string | null;
  userAgent?: string | null;
}) {
  try {
    await db.insert(auditLogs).values({
      userId: params.userId,
      action: params.action,
      resource: params.resource,
      resourceId: params.resourceId ?? null,
      success: params.success,
      metadata: params.metadata ?? null,
      ipAddress: params.ipAddress ?? null,
      userAgent: params.userAgent ?? null,
    });
  } catch {
    // Avoid failing primary request due to audit insertion issues.
  }
}
