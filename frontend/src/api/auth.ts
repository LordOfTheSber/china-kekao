import { AxiosError } from "axios";

import { api } from "@/api/client";
import type { TokenPair } from "@/api/types";

export interface AuthCredentials {
  email: string;
  password: string;
}

export async function login(credentials: AuthCredentials): Promise<TokenPair> {
  const response = await api.post<TokenPair>("/auth/login", credentials);
  return response.data;
}

export async function register(credentials: AuthCredentials): Promise<TokenPair> {
  const response = await api.post<TokenPair>("/auth/register", credentials);
  return response.data;
}

export function extractErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { message?: string; error?: string } | undefined;
    return data?.message ?? data?.error ?? error.message ?? fallback;
  }
  if (error instanceof Error) return error.message;
  return fallback;
}
