import type { AuthUser } from "@/store/auth";

interface AccessTokenPayload {
  sub: string;
  email?: string;
  role?: AuthUser["role"];
}

function base64UrlDecode(input: string): string {
  const padded = input.replace(/-/g, "+").replace(/_/g, "/");
  const padding = padded.length % 4 === 0 ? "" : "=".repeat(4 - (padded.length % 4));
  return atob(padded + padding);
}

export function decodeAccessToken(token: string): AuthUser | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(base64UrlDecode(parts[1])) as AccessTokenPayload;
    if (!payload.sub || !payload.email || !payload.role) return null;
    return { id: payload.sub, email: payload.email, role: payload.role };
  } catch {
    return null;
  }
}
