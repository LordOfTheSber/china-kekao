import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { fetchDashboard } from "@/api/stats";
import type { DashboardStats } from "@/api/types";
import { HanziLoader } from "@/components/HanziLoader";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { BrushDivider } from "@/components/ui/brush-divider";
import { Seal } from "@/components/ui/seal";
import { cn } from "@/lib/utils";

function pickCopy(stats: DashboardStats): { title: string; body: string } {
  const due = stats.dueTodayCount;
  if (due === 0) {
    return {
      title: "All caught up",
      body: "Nothing is due. Browse decks, or come back tomorrow with fresh ink.",
    };
  }
  if (due <= 5) {
    return {
      title: "A short walk today",
      body: "Just a handful of cards. You can finish before the kettle boils.",
    };
  }
  if (due <= 20) {
    return {
      title: "A comfortable pace",
      body: "Take it one stroke at a time. The brush moves with you.",
    };
  }
  if (stats.accuracy7d >= 0.9) {
    return {
      title: "Heavy queue, steady hand",
      body: "You've been accurate lately — keep that rhythm, you'll be through it.",
    };
  }
  return {
    title: "A long road today",
    body: "Don't push for perfect — push for present. One card at a time.",
  };
}

export function DashboardPage() {
  const { data, isLoading, isError, refetch, isRefetching } = useQuery({
    queryKey: ["dashboard"],
    queryFn: fetchDashboard,
  });

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <HanziLoader size={120} label="Reading the day…" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <Card className="max-w-xl mx-auto">
        <CardContent className="pt-6 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-destructive">Could not load your stats.</p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isRefetching}
          >
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  const copy = pickCopy(data);
  const due = data.dueTodayCount;
  const accuracyPct = Math.round(data.accuracy7d * 100);

  return (
    <div className="flex flex-col items-center gap-10 py-6 sm:py-10">
      <header className="flex flex-col items-center gap-3 text-center max-w-xl">
        <span className="text-xs uppercase tracking-[0.4em] text-ink-soft">
          今日 — Today
        </span>
        <h1 className="font-display font-bold leading-none text-ink text-[16vw] sm:text-[10rem] tabular-nums">
          {due}
        </h1>
        <p className="text-sm text-ink-soft uppercase tracking-widest">
          {due === 1 ? "card due" : "cards due"}
        </p>
      </header>

      <div className="flex flex-col items-center gap-3 text-center max-w-md">
        <h2 className="font-hanzi text-2xl text-ink">{copy.title}</h2>
        <p className="text-sm text-ink-soft text-balance">{copy.body}</p>
      </div>

      <Button
        asChild
        size="lg"
        className="h-14 px-10 text-base shadow-seal"
        disabled={due === 0}
      >
        <Link to="/study">
          {due === 0 ? "Practice a deck →" : "Start studying →"}
        </Link>
      </Button>

      <BrushDivider className="max-w-md" />

      <Ribbon
        items={[
          {
            label: "streak",
            value: `${data.currentStreak}`,
            hint: data.currentStreak === 1 ? "day" : "days",
            tone: data.currentStreak >= 7 ? "seal" : "default",
          },
          {
            label: "accuracy 7d",
            value: `${accuracyPct}%`,
            tone: accuracyPct >= 90 ? "success" : "default",
          },
          {
            label: "learned",
            value: data.learnedTotal.toLocaleString(),
          },
          {
            label: "new available",
            value: data.newAvailableCount.toLocaleString(),
          },
        ]}
      />

      {data.currentStreak >= 7 ? (
        <div className="flex items-center gap-2 text-sm text-ink-soft">
          <Seal size="sm" shape="round" tilt={false}>
            勤
          </Seal>
          Diligent week — keep going.
        </div>
      ) : null}
    </div>
  );
}

type RibbonItem = {
  label: string;
  value: string;
  hint?: string;
  tone?: "default" | "success" | "seal";
};

function Ribbon({ items }: { items: RibbonItem[] }) {
  return (
    <div className="flex flex-wrap items-end justify-center gap-x-8 gap-y-4 max-w-2xl">
      {items.map((it) => (
        <div key={it.label} className="flex flex-col items-center min-w-[5rem]">
          <span
            className={cn(
              "text-3xl font-semibold tabular-nums leading-none",
              it.tone === "seal" && "text-seal",
              it.tone === "success" && "text-success",
              (!it.tone || it.tone === "default") && "text-ink",
            )}
          >
            {it.value}
            {it.hint ? (
              <span className="ml-1 text-xs font-normal text-ink-soft">
                {it.hint}
              </span>
            ) : null}
          </span>
          <span className="text-[10px] uppercase tracking-widest text-ink-soft mt-1">
            {it.label}
          </span>
        </div>
      ))}
    </div>
  );
}
