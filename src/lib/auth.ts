import { db } from "@/db";
import { userSessions, users, type User } from "@/db/schema";
import { and, eq, gt } from "drizzle-orm";
import { cookies } from "next/headers";
import { randomBytes, scryptSync, timingSafeEqual, createHash } from "node:crypto";

const SESSION_COOKIE_NAME = "nhatky_session";
const SESSION_TTL_MS = 1000 * 60 * 60 * 24 * 30;

export type SessionUser = {
  id: number;
  email: string;
  fullName: string;
  role: "user" | "admin";
};

export function normalizeEmail(value: string): string {
  return value.trim().toLowerCase();
}

export function hashPassword(password: string): string {
  const salt = randomBytes(16).toString("hex");
  const key = scryptSync(password, salt, 64).toString("hex");
  return `scrypt:${salt}:${key}`;
}

export function verifyPassword(password: string, passwordHash: string): boolean {
  const [algo, salt, keyHex] = passwordHash.split(":");
  if (algo !== "scrypt" || !salt || !keyHex) return false;

  const computed = scryptSync(password, salt, 64);
  const saved = Buffer.from(keyHex, "hex");

  if (computed.length !== saved.length) return false;
  return timingSafeEqual(computed, saved);
}

export function createSessionToken(): string {
  return randomBytes(48).toString("base64url");
}

export function hashSessionToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

export async function createUserSession(params: {
  userId: number;
  ipAddress: string;
  userAgent: string | null;
}) {
  const rawToken = createSessionToken();
  const tokenHash = hashSessionToken(rawToken);
  const expiresAt = new Date(Date.now() + SESSION_TTL_MS);

  await db.insert(userSessions).values({
    userId: params.userId,
    tokenHash,
    expiresAt,
    ipAddress: params.ipAddress,
    userAgent: params.userAgent,
  });

  const cookieStore = await cookies();
  cookieStore.set(SESSION_COOKIE_NAME, rawToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    expires: expiresAt,
  });
}

export async function clearUserSession() {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE_NAME)?.value;

  if (token) {
    const tokenHash = hashSessionToken(token);
    await db.delete(userSessions).where(eq(userSessions.tokenHash, tokenHash));
  }

  cookieStore.set(SESSION_COOKIE_NAME, "", {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    expires: new Date(0),
  });
}

export async function getSessionUser(): Promise<SessionUser | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE_NAME)?.value;
  if (!token) return null;

  const tokenHash = hashSessionToken(token);

  const rows = await db
    .select({
      sessionId: userSessions.id,
      userId: users.id,
      email: users.email,
      fullName: users.fullName,
      role: users.role,
      expiresAt: userSessions.expiresAt,
    })
    .from(userSessions)
    .innerJoin(users, eq(userSessions.userId, users.id))
    .where(and(eq(userSessions.tokenHash, tokenHash), gt(userSessions.expiresAt, new Date())))
    .limit(1);

  const row = rows[0];
  if (!row) return null;

  await db
    .update(userSessions)
    .set({ lastSeenAt: new Date() })
    .where(eq(userSessions.id, row.sessionId));

  return {
    id: row.userId,
    email: row.email,
    fullName: row.fullName,
    role: row.role,
  };
}

export async function requireSessionUser(): Promise<SessionUser> {
  const user = await getSessionUser();
  if (!user) {
    throw new Error("UNAUTHORIZED");
  }
  return user;
}

export function assertRole(user: SessionUser, roles: Array<"user" | "admin">): void {
  if (!roles.includes(user.role)) {
    throw new Error("FORBIDDEN");
  }
}

export function toPublicUser(user: User): SessionUser {
  return {
    id: user.id,
    email: user.email,
    fullName: user.fullName,
    role: user.role,
  };
}
