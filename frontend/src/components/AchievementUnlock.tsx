import { useEffect, useState } from "react";

import type { AchievementView } from "@/api/achievements";
import { Seal } from "@/components/ui/seal";
import { cn } from "@/lib/utils";

interface Props {
  unlocks: AchievementView[];
  onDone?: () => void;
}

/**
 * Full-screen-ish floating banner that announces newly unlocked achievements.
 * If multiple are passed, they queue and show one after another.
 */
export function AchievementUnlock({ unlocks, onDone }: Props) {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    setIndex(0);
  }, [unlocks]);

  useEffect(() => {
    if (unlocks.length === 0) return;
    if (index >= unlocks.length) {
      onDone?.();
      return;
    }
    if (navigator.vibrate) {
      try {
        navigator.vibrate([10, 60, 10]);
      } catch {
        /* noop */
      }
    }
    const t = window.setTimeout(() => setIndex((i) => i + 1), 2600);
    return () => window.clearTimeout(t);
  }, [unlocks, index, onDone]);

  if (unlocks.length === 0 || index >= unlocks.length) return null;
  const item = unlocks[index];

  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        "fixed z-50 left-1/2 -translate-x-1/2 top-6 sm:top-10",
        "flex items-center gap-3 rounded-brush border border-seal/30",
        "bg-paper-elevated/95 backdrop-blur px-4 py-3 shadow-tactile",
        "animate-in fade-in-0 slide-in-from-top-4 motion-reduce:animate-none",
      )}
    >
      <Seal size="md" shape="round" tilt={false}>
        {item.glyph}
      </Seal>
      <div className="flex flex-col">
        <span className="text-[10px] uppercase tracking-[0.3em] text-ink-soft">
          New seal unlocked
        </span>
        <span className="font-hanzi text-sm font-bold text-ink">
          {item.name}
        </span>
        <span className="text-xs text-ink-soft max-w-[18rem]">
          {item.description}
        </span>
      </div>
    </div>
  );
}
