import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import HanziWriter from "hanzi-writer";

import { Button } from "@/components/ui/button";
import { BrushDivider } from "@/components/ui/brush-divider";
import { Seal } from "@/components/ui/seal";
import { AchievementUnlock } from "@/components/AchievementUnlock";
import { claimAchievement, type AchievementView } from "@/api/achievements";
import { cn } from "@/lib/utils";

export interface SessionCompleteProps {
  total: number;
  stats: { again: number; hard: number; good: number; easy: number };
  streakAfter?: number;
}

// "Day character" pickers — small celebratory pieces.
function pickHeroChar(total: number, perfect: boolean): { char: string; gloss: string } {
  if (perfect) return { char: "妙", gloss: "marvellous" };
  if (total >= 100) return { char: "百", gloss: "hundred" };
  if (total >= 50) return { char: "勤", gloss: "diligent" };
  if (total >= 20) return { char: "进", gloss: "advance" };
  if (total >= 10) return { char: "学", gloss: "study" };
  return { char: "好", gloss: "good" };
}

export function SessionComplete({ total, stats, streakAfter }: SessionCompleteProps) {
  const correctish = stats.good + stats.easy;
  const accuracy = total > 0 ? Math.round((correctish / total) * 100) : 0;
  const perfect = total > 0 && stats.again === 0 && stats.hard === 0;
  const hero = pickHeroChar(total, perfect);
  const ref = useRef<HTMLDivElement>(null);
  const [bonusUnlocks, setBonusUnlocks] = useState<AchievementView[]>([]);

  useEffect(() => {
    if (total === 0) return;
    const localHour = new Date().getHours();
    const tasks: Array<Promise<AchievementView | null>> = [];

    if (perfect && total >= 5) {
      tasks.push(
        claimAchievement("PERFECT_DAY", {
          totalCount: total,
          againCount: stats.again,
          hardCount: stats.hard,
        }).then((r) => (r.unlocked ? (r as unknown as { achievement: AchievementView }).achievement : null))
          .catch(() => null),
      );
    }
    if (localHour >= 0 && localHour < 5) {
      tasks.push(
        claimAchievement("NIGHT_OWL", { localHour })
          .then((r) => (r.unlocked ? (r as unknown as { achievement: AchievementView }).achievement : null))
          .catch(() => null),
      );
    }
    if (localHour >= 5 && localHour < 8) {
      tasks.push(
        claimAchievement("EARLY_BIRD", { localHour })
          .then((r) => (r.unlocked ? (r as unknown as { achievement: AchievementView }).achievement : null))
          .catch(() => null),
      );
    }
    if (tasks.length === 0) return;
    Promise.all(tasks).then((results) => {
      const unlocked = results.filter((x): x is AchievementView => x !== null);
      if (unlocked.length > 0) setBonusUnlocks(unlocked);
    });
  }, [perfect, total, stats.again, stats.hard]);

  useEffect(() => {
    if (!ref.current) return;
    ref.current.innerHTML = "";
    try {
      const writer = HanziWriter.create(ref.current, hero.char, {
        width: 220,
        height: 220,
        padding: 6,
        showCharacter: false,
        showOutline: true,
        strokeAnimationSpeed: 1.1,
        delayBetweenStrokes: 120,
        strokeColor: "currentColor",
        outlineColor: "transparent",
      });
      writer.animateCharacter();
    } catch {
      /* fallback handled below */
    }
  }, [hero.char]);

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center gap-8 max-w-xl mx-auto text-center">
      <div className="flex flex-col items-center gap-3">
        <span className="text-xs uppercase tracking-[0.4em] text-ink-soft">
          Session complete
        </span>
        <div
          ref={ref}
          aria-hidden
          className={cn("text-seal h-[220px] w-[220px]")}
        >
          {/* hanzi-writer renders the SVG here */}
        </div>
        <div className="font-hanzi text-3xl text-ink">
          {hero.char}
          <span className="ml-3 text-sm text-ink-soft tracking-wide">{hero.gloss}</span>
        </div>
      </div>

      <BrushDivider className="max-w-xs" />

      <p className="text-base text-ink-soft">
        You reviewed <strong className="text-ink">{total}</strong> card
        {total === 1 ? "" : "s"} — accuracy{" "}
        <strong className="text-ink">{accuracy}%</strong>.
      </p>

      <div className="grid grid-cols-4 gap-3 w-full max-w-md">
        <SummaryTile label="Again" value={stats.again} tone="destructive" />
        <SummaryTile label="Hard" value={stats.hard} tone="warning" />
        <SummaryTile label="Good" value={stats.good} tone="success" />
        <SummaryTile label="Easy" value={stats.easy} tone="seal" />
      </div>

      {perfect ? (
        <Seal shape="round" size="lg" title="Perfect session">
          妙
        </Seal>
      ) : null}

      {streakAfter && streakAfter > 1 ? (
        <p className="text-sm text-ink-soft">
          Streak: <strong className="text-ink">{streakAfter} days</strong>
        </p>
      ) : null}

      <div className="flex flex-wrap justify-center gap-2 pt-2">
        <Button asChild size="lg">
          <Link to="/">Back to dashboard</Link>
        </Button>
        <Button asChild variant="outline" size="lg">
          <Link to="/study" reloadDocument>
            Another round
          </Link>
        </Button>
      </div>

      <AchievementUnlock
        unlocks={bonusUnlocks}
        onDone={() => setBonusUnlocks([])}
      />
    </div>
  );
}

const TONE_CLASSES: Record<string, string> = {
  destructive: "text-destructive",
  warning: "text-warning",
  success: "text-success",
  seal: "text-seal",
};

function SummaryTile({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: keyof typeof TONE_CLASSES;
}) {
  return (
    <div className="flex flex-col items-center gap-1 py-3 rounded-brush border border-brush bg-paper-elevated">
      <span className={cn("text-2xl font-semibold tabular-nums", TONE_CLASSES[tone])}>
        {value}
      </span>
      <span className="text-[10px] uppercase tracking-wider text-ink-soft">
        {label}
      </span>
    </div>
  );
}
