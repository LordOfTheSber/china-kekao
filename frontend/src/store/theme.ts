import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

import type { CustomTokens, ThemeId } from "@/lib/themes";
import { DEFAULT_CUSTOM_TOKENS } from "@/lib/themes";

export type MotionPreference = "auto" | "on" | "off";

interface ThemeState {
  themeId: ThemeId;
  customTokens: CustomTokens;
  motion: MotionPreference;
  soundsEnabled: boolean;
  setTheme: (id: ThemeId) => void;
  setCustomTokens: (tokens: CustomTokens) => void;
  patchCustomTokens: (patch: CustomTokens) => void;
  setMotion: (value: MotionPreference) => void;
  setSoundsEnabled: (value: boolean) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      themeId: "xuanzhi",
      customTokens: DEFAULT_CUSTOM_TOKENS,
      motion: "auto",
      soundsEnabled: false,
      setTheme: (themeId) => set({ themeId }),
      setCustomTokens: (customTokens) => set({ customTokens }),
      patchCustomTokens: (patch) =>
        set((prev) => ({ customTokens: { ...prev.customTokens, ...patch } })),
      setMotion: (motion) => set({ motion }),
      setSoundsEnabled: (soundsEnabled) => set({ soundsEnabled }),
    }),
    {
      name: "kekao.theme",
      storage: createJSONStorage(() => localStorage),
    },
  ),
);
