import { useEffect } from "react";

import { getThemeMode } from "@/lib/themes";
import { useThemeStore } from "@/store/theme";

const CUSTOM_TOKEN_KEYS = [
  "paper",
  "paper-elevated",
  "ink",
  "ink-soft",
  "seal",
  "seal-foreground",
  "brush",
  "watermark",
] as const;

function applyTheme(
  themeId: ReturnType<typeof useThemeStore.getState>["themeId"],
  customTokens: ReturnType<typeof useThemeStore.getState>["customTokens"],
  motion: ReturnType<typeof useThemeStore.getState>["motion"],
) {
  const root = document.documentElement;

  if (themeId === "custom") {
    root.setAttribute("data-theme", "custom");
    for (const key of CUSTOM_TOKEN_KEYS) {
      const value = customTokens[key];
      if (value) {
        root.style.setProperty(`--${key}`, value);
      } else {
        root.style.removeProperty(`--${key}`);
      }
    }
  } else {
    root.setAttribute("data-theme", themeId);
    for (const key of CUSTOM_TOKEN_KEYS) {
      root.style.removeProperty(`--${key}`);
    }
  }

  const mode = getThemeMode(themeId);
  if (mode === "dark") {
    root.classList.add("dark");
  } else {
    root.classList.remove("dark");
  }

  if (motion === "off") {
    root.setAttribute("data-motion", "off");
  } else {
    root.removeAttribute("data-motion");
  }
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const themeId = useThemeStore((s) => s.themeId);
  const customTokens = useThemeStore((s) => s.customTokens);
  const motion = useThemeStore((s) => s.motion);

  useEffect(() => {
    const supportsVT =
      typeof document !== "undefined" &&
      "startViewTransition" in document &&
      typeof (document as Document & {
        startViewTransition?: (cb: () => void) => unknown;
      }).startViewTransition === "function";

    if (supportsVT) {
      (
        document as Document & {
          startViewTransition: (cb: () => void) => unknown;
        }
      ).startViewTransition(() => applyTheme(themeId, customTokens, motion));
    } else {
      applyTheme(themeId, customTokens, motion);
    }
  }, [themeId, customTokens, motion]);

  return <>{children}</>;
}
