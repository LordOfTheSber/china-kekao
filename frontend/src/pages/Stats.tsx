import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { fetchOverview, type OverviewView } from "@/api/stats";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const STATE_COLORS: Record<string, string> = {
  New: "#94a3b8",
  Learning: "#f59e0b",
  Review: "#10b981",
  Relearning: "#ef4444",
};

const RANGE_OPTIONS: Array<{ label: string; days: number }> = [
  { label: "7 days", days: 7 },
  { label: "30 days", days: 30 },
  { label: "90 days", days: 90 },
];

export function StatsPage() {
  const [days, setDays] = useState(30);
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["stats-overview", days],
    queryFn: () => fetchOverview(days),
  });

  if (isLoading) {
    return <div className="h-64 rounded bg-muted animate-pulse max-w-3xl mx-auto" />;
  }
  if (isError || !data) {
    return (
      <Card className="max-w-3xl mx-auto">
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load statistics.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6 max-w-3xl mx-auto">
      <div className="flex items-end justify-between gap-2 flex-wrap">
        <div>
          <h1 className="text-2xl font-semibold">Statistics</h1>
          <p className="text-sm text-muted-foreground">
            Reviews per day, accuracy trend, and card-state breakdown.
          </p>
        </div>
        <div className="flex gap-1">
          {RANGE_OPTIONS.map((opt) => (
            <Button
              key={opt.days}
              size="sm"
              variant={days === opt.days ? "default" : "outline"}
              onClick={() => setDays(opt.days)}
            >
              {opt.label}
            </Button>
          ))}
        </div>
      </div>

      <ReviewsPerDayCard overview={data} />
      <AccuracyCard overview={data} />
      <StatesBreakdownCard overview={data} />
    </div>
  );
}

function ReviewsPerDayCard({ overview }: { overview: OverviewView }) {
  const total = useMemo(
    () => overview.daily.reduce((sum, d) => sum + d.total, 0),
    [overview.daily],
  );
  const data = overview.daily.map((d) => ({
    date: shortDate(d.date),
    total: d.total,
    good: d.good,
    again: d.total - d.good,
  }));
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Reviews per day</CardTitle>
        <CardDescription>
          {total} reviews over the last {overview.days} days.
        </CardDescription>
      </CardHeader>
      <CardContent className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
            <XAxis dataKey="date" tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
            <Tooltip />
            <Legend />
            <Bar dataKey="good" name="Good/Easy" stackId="r" fill="#10b981" />
            <Bar dataKey="again" name="Again/Hard" stackId="r" fill="#ef4444" />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

function AccuracyCard({ overview }: { overview: OverviewView }) {
  const data = overview.daily.map((d) => ({
    date: shortDate(d.date),
    accuracy: Math.round(d.accuracy * 100),
    total: d.total,
  }));
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Accuracy by day</CardTitle>
        <CardDescription>Share of Good and Easy ratings.</CardDescription>
      </CardHeader>
      <CardContent className="h-56">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
            <XAxis dataKey="date" tick={{ fontSize: 11 }} />
            <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 11 }} />
            <Tooltip formatter={(value) => `${value}%`} />
            <Line type="monotone" dataKey="accuracy" stroke="#0ea5e9" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

function StatesBreakdownCard({ overview }: { overview: OverviewView }) {
  const states = overview.states;
  const data = [
    { name: "New", value: states.newCount },
    { name: "Learning", value: states.learning },
    { name: "Review", value: states.review },
    { name: "Relearning", value: states.relearning },
  ].filter((entry) => entry.value > 0);
  const total = data.reduce((sum, d) => sum + d.value, 0);
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Card states</CardTitle>
        <CardDescription>Distribution across all of your cards.</CardDescription>
      </CardHeader>
      <CardContent className="h-64">
        {total === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-muted-foreground">
            No cards yet. Subscribe to a deck to start studying.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={data} dataKey="value" nameKey="name" innerRadius={48} outerRadius={88} label>
                {data.map((entry) => (
                  <Cell key={entry.name} fill={STATE_COLORS[entry.name] ?? "#94a3b8"} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}

function shortDate(iso: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!m) return iso;
  return `${m[2]}-${m[3]}`;
}
