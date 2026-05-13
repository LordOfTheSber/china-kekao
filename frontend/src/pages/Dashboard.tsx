import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { fetchDashboard } from "@/api/stats";
import type { DashboardStats } from "@/api/types";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface StatItem {
  key: keyof DashboardStats;
  label: string;
  format: (value: number) => string;
  hint?: string;
}

const STATS: StatItem[] = [
  { key: "dueTodayCount", label: "Due today", format: (v) => v.toLocaleString() },
  { key: "newAvailableCount", label: "New available", format: (v) => v.toLocaleString() },
  { key: "learnedTotal", label: "Learned total", format: (v) => v.toLocaleString() },
  { key: "currentStreak", label: "Streak", format: (v) => `${v} day${v === 1 ? "" : "s"}` },
  {
    key: "accuracy7d",
    label: "Accuracy (7d)",
    format: (v) => `${Math.round(v * 100)}%`,
    hint: "Ratings ≥ Good in the last 7 days",
  },
];

export function DashboardPage() {
  const { data, isLoading, isError, refetch, isRefetching } = useQuery({
    queryKey: ["dashboard"],
    queryFn: fetchDashboard,
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="relative overflow-hidden rounded-2xl border bg-gradient-to-br from-primary/10 via-background to-background p-6 sm:p-8 shadow-card flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-2xl sm:text-3xl font-semibold tracking-tight text-balance">
            Welcome back
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Your study progress at a glance.
          </p>
        </div>
        <Button
          asChild
          size="lg"
          disabled={isLoading}
          className="w-full sm:w-auto shrink-0"
        >
          <Link to="/study">Start studying →</Link>
        </Button>
      </div>

      {isError ? (
        <Card>
          <CardContent className="pt-6 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-destructive">
              Could not load your stats.
            </p>
            <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isRefetching}>
              Retry
            </Button>
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-3 sm:gap-4 grid-cols-1 xs:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {STATS.map((stat) => (
          <StatCard
            key={stat.key}
            label={stat.label}
            hint={stat.hint}
            value={data ? stat.format(data[stat.key]) : null}
            loading={isLoading}
          />
        ))}
      </div>
    </div>
  );
}

function StatCard({
  label,
  hint,
  value,
  loading,
}: {
  label: string;
  hint?: string;
  value: string | null;
  loading: boolean;
}) {
  return (
    <Card className="hover:shadow-card-hover">
      <CardHeader className="pb-2">
        <CardTitle className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {label}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {loading || value === null ? (
          <div
            className={cn(
              "h-8 w-24 rounded bg-muted animate-pulse",
              loading ? "" : "opacity-50",
            )}
            aria-hidden
          />
        ) : (
          <p className="text-2xl sm:text-3xl font-semibold tabular-nums truncate">
            {value}
          </p>
        )}
        {hint ? (
          <p className="text-xs text-muted-foreground mt-2">{hint}</p>
        ) : null}
      </CardContent>
    </Card>
  );
}
