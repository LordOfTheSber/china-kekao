import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import HanziWriter from "hanzi-writer";

import { fetchHanziDetail, type HanziDetail, type UserCardSummary } from "@/api/hanzi";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const STATE_LABEL: Record<UserCardSummary["state"], string> = {
  NEW: "New",
  LEARNING: "Learning",
  REVIEW: "Review",
  RELEARNING: "Relearning",
};

const MODE_LABEL: Record<UserCardSummary["mode"], string> = {
  RECOGNITION: "Recognition",
  PRODUCTION: "Production",
};

export function HanziDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["hanzi-detail", id],
    queryFn: () => fetchHanziDetail(id!),
    enabled: Boolean(id),
  });

  if (isLoading) {
    return <DetailSkeleton />;
  }
  if (isError || !data) {
    return (
      <Card>
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load this hanzi.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6 max-w-3xl mx-auto w-full">
      <Card>
        <CardContent className="pt-6 flex flex-col sm:flex-row gap-6 items-center sm:items-start">
          <StrokeAnimation
            character={data.character}
            hasStrokeData={data.hasStrokeData}
          />
          <div className="flex flex-col gap-2 flex-1 min-w-0 text-center sm:text-left">
            <h1 className="text-3xl sm:text-4xl font-semibold leading-tight font-serif" lang="zh-Hans">
              {data.character}
            </h1>
            <div className="flex items-baseline gap-x-3 gap-y-1 flex-wrap justify-center sm:justify-start">
              <span className="text-lg break-words">{data.pinyin}</span>
              {data.hskLevel != null ? (
                <span className="text-xs uppercase tracking-wide text-muted-foreground">
                  HSK {data.hskLevel}
                </span>
              ) : null}
              {data.strokeCount != null ? (
                <span className="text-xs text-muted-foreground">
                  {data.strokeCount} strokes
                </span>
              ) : null}
              {data.frequencyRank != null ? (
                <span className="text-xs text-muted-foreground">
                  freq #{data.frequencyRank}
                </span>
              ) : null}
            </div>
            <p className="text-sm">
              {data.meaningsEn.length ? data.meaningsEn.join(", ") : "No English meanings yet."}
            </p>
            <UserCardsBlock detail={data} />
          </div>
        </CardContent>
      </Card>

      <ExamplesBlock detail={data} />
    </div>
  );
}

function StrokeAnimation({
  character,
  hasStrokeData,
}: {
  character: string;
  hasStrokeData: boolean;
}) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const writerRef = useRef<ReturnType<typeof HanziWriter.create> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [size, setSize] = useState(220);

  useEffect(() => {
    if (!wrapperRef.current) return;
    const el = wrapperRef.current;
    const measure = () => {
      const w = el.getBoundingClientRect().width;
      if (w > 0) {
        setSize(Math.max(200, Math.min(260, Math.round(w))));
      }
    };
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    if (!containerRef.current) return;
    containerRef.current.innerHTML = "";
    setError(null);
    let cancelled = false;
    try {
      const writer = HanziWriter.create(containerRef.current, character, {
        width: size,
        height: size,
        padding: 8,
        showCharacter: true,
        showOutline: true,
        strokeAnimationSpeed: 1,
        delayBetweenStrokes: 200,
        onLoadCharDataError: (err: unknown) => {
          if (cancelled) return;
          setError(err instanceof Error ? err.message : "Stroke data unavailable");
        },
      });
      writerRef.current = writer;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Stroke data unavailable");
    }
    return () => {
      cancelled = true;
      writerRef.current = null;
    };
  }, [character, size]);

  function animate() {
    writerRef.current?.animateCharacter();
  }

  if (!hasStrokeData) {
    return (
      <div className="flex flex-col items-center gap-2 w-full max-w-[240px] mx-auto">
        <div className="aspect-square w-full flex items-center justify-center text-7xl font-serif border rounded-lg bg-muted/30">
          {character}
        </div>
        <p className="text-xs text-muted-foreground">No stroke data available.</p>
      </div>
    );
  }

  return (
    <div
      ref={wrapperRef}
      className="flex flex-col items-center gap-2 w-full max-w-[240px] mx-auto"
    >
      <div
        ref={containerRef}
        className="aspect-square w-full border rounded-lg bg-muted/20 flex items-center justify-center"
      />
      {error ? (
        <p className="text-xs text-destructive">{error}</p>
      ) : (
        <Button size="sm" variant="outline" onClick={animate}>
          Replay stroke order
        </Button>
      )}
    </div>
  );
}

function ExamplesBlock({ detail }: { detail: HanziDetail }) {
  if (detail.examples.length === 0) return null;
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Examples</CardTitle>
        <CardDescription>Sentences using this character.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {detail.examples.map((example, index) => (
          <div key={index} className="border-l-2 border-primary/40 pl-3 flex flex-col gap-1 min-w-0">
            <div className="text-base break-words" lang="zh-Hans">{example.sentence}</div>
            <div className="text-xs text-muted-foreground break-words">{example.pinyin}</div>
            <div className="text-sm break-words">{example.translation}</div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

function UserCardsBlock({ detail }: { detail: HanziDetail }) {
  const cards = detail.userCards;
  const inQueue = cards.length > 0;

  if (inQueue) {
    return (
      <div className="mt-2 flex flex-col gap-1">
        <span className="text-xs uppercase tracking-wide text-muted-foreground">
          Your study state
        </span>
        <ul className="flex flex-wrap gap-2 text-xs">
          {cards.map((c) => (
            <li
              key={c.userCardId}
              className="rounded-full border px-2 py-0.5 bg-accent/40"
            >
              {MODE_LABEL[c.mode]}: <strong>{STATE_LABEL[c.state]}</strong>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div className="mt-2">
      <p className="text-xs text-muted-foreground mb-1">
        Not in your queue yet. Subscribe to a deck containing this hanzi from the
        <a href="/decks" className="underline ml-1">decks page</a>.
      </p>
      <Button
        size="sm"
        variant="outline"
        onClick={() => {
          window.location.href = "/decks";
        }}
      >
        Browse decks
      </Button>
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div className="max-w-3xl mx-auto w-full flex flex-col gap-4">
      <Card>
        <CardContent className="pt-6 flex flex-col sm:flex-row gap-6">
          <div className="aspect-square w-full max-w-[240px] mx-auto sm:mx-0 rounded-lg bg-muted animate-pulse" />
          <div className="flex-1 flex flex-col gap-2">
            <div className="h-8 w-32 rounded bg-muted animate-pulse" />
            <div className="h-4 w-40 rounded bg-muted animate-pulse" />
            <div className="h-4 w-48 rounded bg-muted animate-pulse" />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
