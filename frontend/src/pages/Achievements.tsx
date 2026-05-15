import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";

import { fetchAchievements, type AchievementView } from "@/api/achievements";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { BrushDivider } from "@/components/ui/brush-divider";
import { Seal } from "@/components/ui/seal";
import { HanziLoader } from "@/components/HanziLoader";
import { cn } from "@/lib/utils";

const CATEGORY_LABELS: Record<string, string> = {
  LEARNED: "Master seals",
  STREAK: "Streak milestones",
  REVIEWS: "Review milestones",
  SPECIAL: "Secrets",
};

const CATEGORY_HANZI: Record<string, string> = {
  LEARNED: "印",
  STREAK: "续",
  REVIEWS: "练",
  SPECIAL: "秘",
};

const CATEGORY_ORDER = ["LEARNED", "STREAK", "REVIEWS", "SPECIAL"];

export function AchievementsPage() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["achievements"],
    queryFn: fetchAchievements,
  });

  const grouped = useMemo(() => {
    if (!data) return new Map<string, AchievementView[]>();
    const map = new Map<string, AchievementView[]>();
    for (const cat of CATEGORY_ORDER) map.set(cat, []);
    for (const item of data.items) {
      const list = map.get(item.category) ?? [];
      list.push(item);
      map.set(item.category, list);
    }
    return map;
  }, [data]);

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <HanziLoader size={96} label="Counting your seals…" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <Card className="max-w-xl mx-auto">
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load achievements.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-8 max-w-4xl mx-auto w-full">
      <header className="flex flex-col items-center gap-2 text-center">
        <h1 className="font-hanzi text-3xl sm:text-4xl font-bold tracking-tight text-ink">
          印章 <span className="text-2xl sm:text-3xl">Master seals</span>
        </h1>
        <p className="text-sm text-ink-soft">
          <span className="text-ink font-semibold tabular-nums">{data.unlocked}</span>
          {" "}of <span className="tabular-nums">{data.total}</span> unlocked.
        </p>
      </header>

      {CATEGORY_ORDER.map((cat) => {
        const items = grouped.get(cat) ?? [];
        if (items.length === 0) return null;
        return (
          <section key={cat} className="flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <span className="font-hanzi text-2xl text-seal">
                {CATEGORY_HANZI[cat]}
              </span>
              <h2 className="font-hanzi text-xl font-bold text-ink">
                {CATEGORY_LABELS[cat]}
              </h2>
              <span className="text-xs text-ink-soft tabular-nums">
                {items.filter((i) => i.unlocked).length}/{items.length}
              </span>
            </div>
            <BrushDivider className="opacity-50" />
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
              {items.map((item) => (
                <AchievementCard key={item.code} item={item} />
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}

function AchievementCard({ item }: { item: AchievementView }) {
  const unlocked = item.unlocked;
  return (
    <div
      className={cn(
        "relative rounded-brush border p-4 flex flex-col items-center gap-2 text-center min-h-[160px] transition-all",
        unlocked
          ? "border-seal/40 bg-paper-elevated shadow-card"
          : "border-brush/20 bg-paper/40 opacity-65 grayscale",
      )}
      title={
        item.unlockedAt
          ? `Unlocked ${new Date(item.unlockedAt).toLocaleDateString()}`
          : undefined
      }
    >
      {unlocked ? (
        <Seal size="lg" shape="round" className="mb-1">
          {item.glyph}
        </Seal>
      ) : (
        <div className="h-14 w-14 rounded-full border-2 border-dashed border-brush/40 flex items-center justify-center font-hanzi text-2xl text-ink-soft/50">
          {item.glyph}
        </div>
      )}
      <div className="font-hanzi text-sm font-bold text-ink leading-tight">
        {item.name}
      </div>
      <div className="text-[11px] text-ink-soft leading-snug">
        {item.description}
      </div>
      {item.threshold != null ? (
        <div className="text-[10px] uppercase tracking-wider text-ink-soft mt-auto pt-1 tabular-nums">
          target: {item.threshold.toLocaleString()}
        </div>
      ) : null}
    </div>
  );
}
