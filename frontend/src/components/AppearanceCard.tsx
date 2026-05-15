import { useMemo, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { toast } from "@/components/Toaster";
import { cn } from "@/lib/utils";
import {
  DEFAULT_CUSTOM_TOKENS,
  THEMES,
  THEME_ORDER,
  decodeCustomTheme,
  encodeCustomTheme,
  type ThemeId,
} from "@/lib/themes";
import { useThemeStore, type MotionPreference } from "@/store/theme";

const MOTION_OPTIONS: Array<{
  value: MotionPreference;
  label: string;
  description: string;
}> = [
  { value: "auto", label: "Auto", description: "Follow your system setting." },
  { value: "on", label: "On", description: "Always animate." },
  { value: "off", label: "Off", description: "Disable all motion." },
];

export function AppearanceCard() {
  const themeId = useThemeStore((s) => s.themeId);
  const setTheme = useThemeStore((s) => s.setTheme);
  const motion = useThemeStore((s) => s.motion);
  const setMotion = useThemeStore((s) => s.setMotion);
  const soundsEnabled = useThemeStore((s) => s.soundsEnabled);
  const setSoundsEnabled = useThemeStore((s) => s.setSoundsEnabled);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Appearance</CardTitle>
        <CardDescription>
          Themes, motion, and sound. Stored on this device.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <div className="flex flex-col gap-3">
          <h3 className="text-sm font-semibold text-ink">Theme</h3>
          <div className="grid grid-cols-2 xs:grid-cols-3 sm:grid-cols-4 gap-3">
            {THEME_ORDER.map((id) => (
              <ThemeTile
                key={id}
                themeId={id}
                selected={themeId === id}
                onSelect={() => setTheme(id)}
              />
            ))}
            <ThemeTile
              themeId="custom"
              selected={themeId === "custom"}
              onSelect={() => setTheme("custom")}
            />
          </div>
        </div>

        {themeId === "custom" ? <CustomThemePanel /> : null}

        <div className="flex flex-col gap-2">
          <h3 className="text-sm font-semibold text-ink">Motion</h3>
          <div className="grid sm:grid-cols-3 gap-2">
            {MOTION_OPTIONS.map((opt) => (
              <Button
                key={opt.value}
                type="button"
                variant="outline"
                onClick={() => setMotion(opt.value)}
                className={cn(
                  "h-auto flex flex-col items-start gap-1 p-3 text-left whitespace-normal w-full min-w-0",
                  motion === opt.value && "ring-2 ring-seal border-seal bg-seal/5",
                )}
              >
                <span className="text-sm font-semibold">{opt.label}</span>
                <span className="text-xs text-ink-soft">{opt.description}</span>
              </Button>
            ))}
          </div>
        </div>

        <label className="flex items-start gap-3 cursor-pointer select-none">
          <input
            type="checkbox"
            className="mt-1 h-4 w-4 accent-[hsl(var(--seal))]"
            checked={soundsEnabled}
            onChange={(e) => setSoundsEnabled(e.target.checked)}
          />
          <span className="flex flex-col">
            <span className="text-sm font-semibold text-ink">Sounds</span>
            <span className="text-xs text-ink-soft">
              Optional gong on correct answers (off by default).
            </span>
          </span>
        </label>
      </CardContent>
    </Card>
  );
}

function ThemeTile({
  themeId,
  selected,
  onSelect,
}: {
  themeId: ThemeId;
  selected: boolean;
  onSelect: () => void;
}) {
  const preset = themeId === "custom" ? null : THEMES[themeId];
  const label = preset?.label ?? "Custom";
  const swatchPaper = preset?.swatchPaper ?? "linear-gradient(135deg, #F6F1E6 0%, #C8161D 50%, #1B1B1A 100%)";
  const swatchInk = preset?.swatchInk ?? "#1B1B1A";
  const swatchSeal = preset?.swatchSeal ?? "#C8161D";

  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        "w-full min-w-0 rounded-brush border p-3 text-left transition-all",
        "hover:-translate-y-0.5 motion-reduce:hover:transform-none",
        selected
          ? "border-seal ring-2 ring-seal shadow-seal"
          : "border-brush/30",
      )}
      style={{
        background: preset ? swatchPaper : undefined,
        backgroundImage: preset ? undefined : swatchPaper,
        color: swatchInk,
      }}
      aria-pressed={selected}
    >
      <div className="flex items-center justify-between">
        <span
          className="font-hanzi text-3xl font-bold leading-none"
          style={{ color: swatchInk }}
        >
          永
        </span>
        <span
          className="h-4 w-4 rounded-full"
          style={{ background: swatchSeal }}
          aria-hidden
        />
      </div>
      <div
        className="mt-2 text-[11px] font-semibold leading-tight"
        style={{ color: swatchInk }}
      >
        {label}
      </div>
    </button>
  );
}

const HSL_FIELDS: Array<{
  key: keyof typeof DEFAULT_CUSTOM_TOKENS;
  label: string;
}> = [
  { key: "paper", label: "Paper" },
  { key: "ink", label: "Ink" },
  { key: "seal", label: "Seal" },
  { key: "brush", label: "Brush" },
];

function parseHsl(value: string): { h: number; s: number; l: number } | null {
  const m = value.match(/^\s*(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)%\s+(\d+(?:\.\d+)?)%\s*$/);
  if (!m) return null;
  return { h: Number(m[1]), s: Number(m[2]), l: Number(m[3]) };
}

function formatHsl(h: number, s: number, l: number): string {
  return `${h} ${s}% ${l}%`;
}

function CustomThemePanel() {
  const customTokens = useThemeStore((s) => s.customTokens);
  const patchCustomTokens = useThemeStore((s) => s.patchCustomTokens);
  const setCustomTokens = useThemeStore((s) => s.setCustomTokens);
  const [importValue, setImportValue] = useState("");

  const exported = useMemo(() => encodeCustomTheme(customTokens), [customTokens]);

  function handleSliderChange(
    key: keyof typeof DEFAULT_CUSTOM_TOKENS,
    component: "h" | "s" | "l",
    next: number,
  ) {
    const current = parseHsl(customTokens[key] ?? DEFAULT_CUSTOM_TOKENS[key]);
    const base = current ?? { h: 0, s: 0, l: 50 };
    const updated = { ...base, [component]: next };
    patchCustomTokens({ [key]: formatHsl(updated.h, updated.s, updated.l) });
  }

  function handleImport() {
    const decoded = decodeCustomTheme(importValue);
    if (!decoded) {
      toast({
        title: "Invalid theme string",
        description: "Paste the encoded value from another device.",
        variant: "destructive",
      });
      return;
    }
    setCustomTokens({ ...DEFAULT_CUSTOM_TOKENS, ...decoded });
    setImportValue("");
    toast({ title: "Custom theme imported" });
  }

  function handleCopy() {
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(exported).then(
        () => toast({ title: "Theme copied", description: "Paste it on another device to share." }),
        () => toast({ title: "Could not copy", variant: "destructive" }),
      );
    }
  }

  return (
    <div className="rounded-brush border border-brush/40 bg-paper-elevated p-4 flex flex-col gap-4">
      <h4 className="text-sm font-semibold text-ink">Custom palette</h4>
      <div className="flex flex-col gap-3">
        {HSL_FIELDS.map((field) => {
          const value =
            parseHsl(customTokens[field.key] ?? DEFAULT_CUSTOM_TOKENS[field.key]) ?? {
              h: 0,
              s: 0,
              l: 50,
            };
          return (
            <div key={field.key} className="flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wide text-ink-soft">
                  {field.label}
                </span>
                <span
                  className="h-5 w-12 rounded border border-brush/30"
                  style={{ background: `hsl(${value.h} ${value.s}% ${value.l}%)` }}
                  aria-hidden
                />
              </div>
              <SliderRow
                label="H"
                min={0}
                max={360}
                value={value.h}
                onChange={(v) => handleSliderChange(field.key, "h", v)}
              />
              <SliderRow
                label="S"
                min={0}
                max={100}
                suffix="%"
                value={value.s}
                onChange={(v) => handleSliderChange(field.key, "s", v)}
              />
              <SliderRow
                label="L"
                min={0}
                max={100}
                suffix="%"
                value={value.l}
                onChange={(v) => handleSliderChange(field.key, "l", v)}
              />
            </div>
          );
        })}
      </div>

      <div className="flex flex-col gap-2">
        <label className="text-xs font-semibold uppercase tracking-wide text-ink-soft">
          Share / import
        </label>
        <div className="flex flex-col sm:flex-row gap-2">
          <Input
            value={importValue}
            onChange={(e) => setImportValue(e.target.value)}
            placeholder="Paste an encoded theme to import"
            className="flex-1"
          />
          <Button
            size="sm"
            variant="outline"
            onClick={handleImport}
            disabled={!importValue.trim()}
          >
            Import
          </Button>
          <Button size="sm" variant="outline" onClick={handleCopy}>
            Copy mine
          </Button>
        </div>
        <textarea
          readOnly
          value={exported}
          className="font-mono text-[11px] rounded-md border border-brush/30 bg-paper p-2 text-ink-soft min-h-[44px] resize-none"
          aria-label="Encoded custom theme"
        />
      </div>

      <Button
        size="sm"
        variant="ghost"
        onClick={() => setCustomTokens(DEFAULT_CUSTOM_TOKENS)}
      >
        Reset to defaults
      </Button>
    </div>
  );
}

function SliderRow({
  label,
  min,
  max,
  value,
  onChange,
  suffix,
}: {
  label: string;
  min: number;
  max: number;
  value: number;
  onChange: (v: number) => void;
  suffix?: string;
}) {
  return (
    <label className="flex items-center gap-2 text-xs">
      <span className="w-4 text-ink-soft font-semibold">{label}</span>
      <input
        type="range"
        min={min}
        max={max}
        step={1}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="flex-1 accent-[hsl(var(--seal))]"
      />
      <span className="font-mono w-12 text-right tabular-nums text-ink-soft">
        {Math.round(value)}
        {suffix ?? ""}
      </span>
    </label>
  );
}
