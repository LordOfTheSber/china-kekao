import { useEffect, useMemo, useRef } from "react";
import HanziWriter from "hanzi-writer";

import { cn } from "@/lib/utils";

const LOADER_CHARS: Array<{ char: string; pinyin: string; meaning: string }> = [
  { char: "一", pinyin: "yī", meaning: "one" },
  { char: "二", pinyin: "èr", meaning: "two" },
  { char: "三", pinyin: "sān", meaning: "three" },
  { char: "人", pinyin: "rén", meaning: "person" },
  { char: "口", pinyin: "kǒu", meaning: "mouth" },
  { char: "日", pinyin: "rì", meaning: "sun, day" },
  { char: "月", pinyin: "yuè", meaning: "moon, month" },
  { char: "山", pinyin: "shān", meaning: "mountain" },
  { char: "水", pinyin: "shuǐ", meaning: "water" },
  { char: "火", pinyin: "huǒ", meaning: "fire" },
  { char: "木", pinyin: "mù", meaning: "tree, wood" },
  { char: "中", pinyin: "zhōng", meaning: "middle" },
  { char: "文", pinyin: "wén", meaning: "writing" },
  { char: "好", pinyin: "hǎo", meaning: "good" },
  { char: "学", pinyin: "xué", meaning: "to study" },
  { char: "心", pinyin: "xīn", meaning: "heart" },
];

type Props = {
  size?: number;
  label?: string;
  className?: string;
};

export function HanziLoader({ size = 96, label, className }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const entry = useMemo(
    () => LOADER_CHARS[Math.floor(Math.random() * LOADER_CHARS.length)],
    [],
  );

  useEffect(() => {
    if (!ref.current) return;
    ref.current.innerHTML = "";
    let cancelled = false;
    let writer: ReturnType<typeof HanziWriter.create> | null = null;
    try {
      writer = HanziWriter.create(ref.current, entry.char, {
        width: size,
        height: size,
        padding: 4,
        showCharacter: false,
        showOutline: true,
        strokeAnimationSpeed: 1.3,
        delayBetweenStrokes: 100,
        strokeColor: "currentColor",
        outlineColor: "transparent",
      });
      writer.loopCharacterAnimation();
    } catch {
      // Silent fallback — loader is decorative.
    }
    return () => {
      cancelled = true;
      void cancelled;
      writer = null;
    };
  }, [entry.char, size]);

  return (
    <div
      role="status"
      aria-live="polite"
      aria-label={label ?? `Loading. Practice character: ${entry.char} (${entry.pinyin}) — ${entry.meaning}`}
      className={cn(
        "flex flex-col items-center justify-center gap-2 text-ink",
        className,
      )}
    >
      <div ref={ref} style={{ width: size, height: size }} className="text-ink" />
      <div className="flex flex-col items-center text-center">
        <span className="text-xs font-medium tracking-wide text-ink-soft">
          {entry.pinyin}
        </span>
        <span className="text-[10px] uppercase tracking-wider text-ink-soft/80">
          {entry.meaning}
        </span>
      </div>
      {label ? (
        <span className="text-xs text-ink-soft">{label}</span>
      ) : null}
    </div>
  );
}
