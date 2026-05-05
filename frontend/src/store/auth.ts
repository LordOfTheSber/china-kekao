import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export interface AuthUser {
  id: string;
  email: string;
  role: "ROLE_USER" | "ROLE_ADMIN";
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

interface AuthState {
  user: AuthUser | null;
  tokens: AuthTokens | null;
  setSession: (payload: { user: AuthUser; tokens: AuthTokens }) => void;
  setTokens: (tokens: AuthTokens) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      tokens: null,
      setSession: ({ user, tokens }) => set({ user, tokens }),
      setTokens: (tokens) => set({ tokens }),
      clear: () => set({ user: null, tokens: null }),
    }),
    {
      name: "kekao.auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, tokens: state.tokens }),
    },
  ),
);

export function getAccessToken(): string | null {
  return useAuthStore.getState().tokens?.accessToken ?? null;
}

export function getRefreshToken(): string | null {
  return useAuthStore.getState().tokens?.refreshToken ?? null;
}
