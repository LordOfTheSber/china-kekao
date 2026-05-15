/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    container: {
      center: true,
      padding: {
        DEFAULT: "1rem",
        sm: "1.25rem",
        lg: "2rem",
      },
      screens: { "2xl": "1280px" },
    },
    screens: {
      xs: "420px",
      sm: "640px",
      md: "768px",
      lg: "1024px",
      xl: "1280px",
      "2xl": "1536px",
    },
    extend: {
      fontFamily: {
        sans: ["Inter", "system-ui", "-apple-system", "sans-serif"],
        hanzi: ['"Noto Serif SC"', "ui-serif", "Georgia", "serif"],
        display: ["var(--font-display)", '"Noto Serif SC"', "Inter", "serif"],
        mono: [
          '"JetBrains Mono"',
          "ui-monospace",
          "SFMono-Regular",
          "Menlo",
          "monospace",
        ],
        rounded: ['"Nunito"', '"Quicksand"', "Inter", "sans-serif"],
      },
      boxShadow: {
        card: "0 1px 2px hsl(var(--ink) / 0.04), 0 4px 12px -2px hsl(var(--ink) / 0.06)",
        "card-hover":
          "0 2px 4px hsl(var(--ink) / 0.06), 0 10px 24px -4px hsl(var(--ink) / 0.10)",
        tactile:
          "0 1px 0 hsl(var(--ink) / 0.04), 4px 6px 0 -1px hsl(var(--ink) / 0.08), 0 12px 28px -10px hsl(var(--ink) / 0.18)",
        seal: "0 2px 0 hsl(var(--seal) / 0.18), 0 6px 18px -6px hsl(var(--seal) / 0.45)",
      },
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        paper: {
          DEFAULT: "hsl(var(--paper))",
          elevated: "hsl(var(--paper-elevated))",
        },
        ink: {
          DEFAULT: "hsl(var(--ink))",
          soft: "hsl(var(--ink-soft))",
        },
        seal: {
          DEFAULT: "hsl(var(--seal))",
          foreground: "hsl(var(--seal-foreground))",
        },
        brush: "hsl(var(--brush))",
        watermark: "hsl(var(--watermark))",
        success: {
          DEFAULT: "hsl(var(--success))",
          foreground: "hsl(var(--success-foreground))",
        },
        warning: {
          DEFAULT: "hsl(var(--warning))",
          foreground: "hsl(var(--warning-foreground))",
        },
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
        brush: "var(--brush-radius)",
      },
      keyframes: {
        "seal-pop": {
          "0%": { transform: "scale(0.5) rotate(-8deg)", opacity: "0" },
          "30%": { transform: "scale(1.08) rotate(-3deg)", opacity: "1" },
          "70%": { transform: "scale(1) rotate(-3deg)", opacity: "1" },
          "100%": { transform: "scale(1) rotate(-3deg)", opacity: "0" },
        },
        "gentle-wobble": {
          "0%, 100%": { transform: "rotate(0deg)" },
          "20%": { transform: "rotate(-2deg)" },
          "40%": { transform: "rotate(2deg)" },
          "60%": { transform: "rotate(-1deg)" },
          "80%": { transform: "rotate(1deg)" },
        },
        "ink-pulse": {
          "0%, 100%": { transform: "scale(1)" },
          "50%": { transform: "scale(1.04)" },
        },
        "petal-drift": {
          "0%": { transform: "translate3d(0, -10vh, 0) rotate(0deg)", opacity: "0" },
          "10%": { opacity: "0.6" },
          "100%": { transform: "translate3d(20vw, 110vh, 0) rotate(420deg)", opacity: "0" },
        },
        "scanline-shift": {
          "0%": { backgroundPosition: "0 0" },
          "100%": { backgroundPosition: "0 4px" },
        },
        "brush-wipe": {
          "0%": { clipPath: "inset(0 100% 0 0)" },
          "100%": { clipPath: "inset(0 0 0 0)" },
        },
        "typing-blink": {
          "0%, 49%": { borderRightColor: "currentColor" },
          "50%, 100%": { borderRightColor: "transparent" },
        },
      },
      animation: {
        "seal-pop": "seal-pop 700ms cubic-bezier(0.34, 1.56, 0.64, 1) forwards",
        "gentle-wobble": "gentle-wobble 480ms ease-in-out",
        "ink-pulse": "ink-pulse 420ms ease-out",
        "petal-drift": "petal-drift linear infinite",
        "scanline-shift": "scanline-shift 6s linear infinite",
        "brush-wipe": "brush-wipe 420ms cubic-bezier(0.65, 0, 0.35, 1) forwards",
        "typing-blink": "typing-blink 900ms steps(2) infinite",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};
