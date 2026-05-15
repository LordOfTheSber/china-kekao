export type ThemeId =
  | "xuanzhi"
  | "midnight"
  | "teahouse"
  | "neon"
  | "ghibli"
  | "hacker"
  | "custom";

export type ThemeMode = "light" | "dark";

export type ThemeTokens = {
  paper: string;
  "paper-elevated": string;
  ink: string;
  "ink-soft": string;
  seal: string;
  "seal-foreground": string;
  brush: string;
  watermark: string;
};

export type CustomTokens = Partial<ThemeTokens>;

export interface ThemePreset {
  id: ThemeId;
  label: string;
  description: string;
  mode: ThemeMode;
  swatchPaper: string;
  swatchInk: string;
  swatchSeal: string;
}

export const THEMES: Record<Exclude<ThemeId, "custom">, ThemePreset> = {
  xuanzhi: {
    id: "xuanzhi",
    label: "宣纸 Xuanzhi",
    description: "Paper & ink. The classical foundation.",
    mode: "light",
    swatchPaper: "#F4EEDD",
    swatchInk: "#1E1C1A",
    swatchSeal: "#C8161D",
  },
  midnight: {
    id: "midnight",
    label: "墨夜 Midnight",
    description: "Deep indigo, phosphor glow.",
    mode: "dark",
    swatchPaper: "#101434",
    swatchInk: "#EFEADF",
    swatchSeal: "#9EE493",
  },
  teahouse: {
    id: "teahouse",
    label: "茶室 Teahouse",
    description: "Ochre, sepia, warm wood.",
    mode: "light",
    swatchPaper: "#F1E1C4",
    swatchInk: "#3D2C1B",
    swatchSeal: "#A54F1F",
  },
  neon: {
    id: "neon",
    label: "霓虹胡同 Neon Hutong",
    description: "Cyberpunk Shanghai, scanlines & glitch.",
    mode: "dark",
    swatchPaper: "#0A0A14",
    swatchInk: "#E8E7F4",
    swatchSeal: "#FF2D6F",
  },
  ghibli: {
    id: "ghibli",
    label: "宫崎 Ghibli",
    description: "Pastel watercolours and drifting petals.",
    mode: "light",
    swatchPaper: "#EAF6F2",
    swatchInk: "#3A4D55",
    swatchSeal: "#E89AA5",
  },
  hacker: {
    id: "hacker",
    label: "终端 Terminal",
    description: "Monospace green-on-black.",
    mode: "dark",
    swatchPaper: "#0B0F0B",
    swatchInk: "#62FF7E",
    swatchSeal: "#62FF7E",
  },
};

export const THEME_ORDER: Array<Exclude<ThemeId, "custom">> = [
  "xuanzhi",
  "midnight",
  "teahouse",
  "neon",
  "ghibli",
  "hacker",
];

export const DEFAULT_CUSTOM_TOKENS: ThemeTokens = {
  paper: "39 38% 94%",
  "paper-elevated": "40 45% 97%",
  ink: "30 6% 12%",
  "ink-soft": "30 5% 32%",
  seal: "358 78% 44%",
  "seal-foreground": "40 45% 97%",
  brush: "30 6% 12%",
  watermark: "30 6% 12%",
};

export function encodeCustomTheme(tokens: CustomTokens): string {
  const json = JSON.stringify(tokens);
  if (typeof window === "undefined") return "";
  return window.btoa(unescape(encodeURIComponent(json)));
}

export function decodeCustomTheme(encoded: string): CustomTokens | null {
  if (typeof window === "undefined") return null;
  try {
    const json = decodeURIComponent(escape(window.atob(encoded.trim())));
    const parsed = JSON.parse(json);
    if (parsed && typeof parsed === "object") {
      return parsed as CustomTokens;
    }
  } catch {
    return null;
  }
  return null;
}

export function getThemeMode(id: ThemeId): ThemeMode {
  if (id === "custom") return "light";
  return THEMES[id].mode;
}
