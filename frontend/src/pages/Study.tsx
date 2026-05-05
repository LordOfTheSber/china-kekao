import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { fetchStudySession, postReview } from "@/api/study";
import { extractErrorMessage } from "@/api/auth";
import type { Rating, StudyCard } from "@/api/types";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { toast } from "@/components/Toaster";
import { cn } from "@/lib/utils";
import { gradeAnswer, type GradeResult } from "@/pages/study/grading";

interface SessionStat {
  again: number;
  hard: number;
  good: number;
  easy: number;
}

const RATINGS: Array<{
  rating: Rating;
  label: string;
  shortcut: string;
  className: string;
}> = [
  { rating: "AGAIN", label: "Again", shortcut: "1", className: "bg-rose-600 text-white hover:bg-rose-600/90" },
  { rating: "HARD", label: "Hard", shortcut: "2", className: "bg-amber-500 text-white hover:bg-amber-500/90" },
  { rating: "GOOD", label: "Good", shortcut: "3", className: "bg-emerald-600 text-white hover:bg-emerald-600/90" },
  { rating: "EASY", label: "Easy", shortcut: "4", className: "bg-sky-600 text-white hover:bg-sky-600/90" },
];

export function StudyPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["study-session"],
    queryFn: fetchStudySession,
    refetchOnMount: "always",
    staleTime: 0,
  });

  if (isLoading) {
    return <SessionSkeleton />;
  }
  if (isError) {
    return (
      <Card>
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load your session.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </CardContent>
      </Card>
    );
  }
  if (!data || data.length === 0) {
    return <EmptyQueue />;
  }

  return (
    <SessionRunner
      cards={data}
      onFinished={() => {
        queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      }}
    />
  );
}

function SessionRunner({
  cards,
  onFinished,
}: {
  cards: StudyCard[];
  onFinished: () => void;
}) {
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [grade, setGrade] = useState<GradeResult | null>(null);
  const [pinyin, setPinyin] = useState("");
  const [meaning, setMeaning] = useState("");
  const [stats, setStats] = useState<SessionStat>({ again: 0, hard: 0, good: 0, easy: 0 });
  const [pendingRating, setPendingRating] = useState<Rating | null>(null);
  const [showNearDialog, setShowNearDialog] = useState(false);
  const startedAtRef = useRef<number>(performance.now());
  const pinyinInputRef = useRef<HTMLInputElement>(null);

  const card = cards[index];
  const finished = index >= cards.length;

  const reviewMutation = useMutation({
    mutationFn: postReview,
    onError: (error) => {
      toast({
        title: "Could not save review",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  useEffect(() => {
    if (finished) {
      onFinished();
      return;
    }
    startedAtRef.current = performance.now();
    setRevealed(false);
    setGrade(null);
    setPinyin("");
    setMeaning("");
    setPendingRating(null);
    setShowNearDialog(false);
    pinyinInputRef.current?.focus();
  }, [index, finished, onFinished]);

  const acceptedMeanings = useMemo(() => card?.meanings ?? [], [card]);

  function handleCheck() {
    if (!card || revealed) return;
    const result = gradeAnswer(card.pinyin, acceptedMeanings, pinyin, meaning);
    setGrade(result);
    setRevealed(true);
  }

  function applyRating(rating: Rating) {
    if (!card) return;
    const responseTimeMs = Math.max(0, Math.round(performance.now() - startedAtRef.current));
    reviewMutation.mutate({
      userCardId: card.userCardId,
      mode: card.mode,
      rating,
      responseTimeMs,
      hintCount: 0,
      strokeMistakes: 0,
    });
    setStats((prev) => ({
      again: prev.again + (rating === "AGAIN" ? 1 : 0),
      hard: prev.hard + (rating === "HARD" ? 1 : 0),
      good: prev.good + (rating === "GOOD" ? 1 : 0),
      easy: prev.easy + (rating === "EASY" ? 1 : 0),
    }));
    setIndex((i) => i + 1);
  }

  function handleRate(rating: Rating) {
    if (!revealed) return;
    if (grade?.outcome === "NEAR" && !pendingRating) {
      setPendingRating(rating);
      setShowNearDialog(true);
      return;
    }
    applyRating(rating);
  }

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.target instanceof HTMLInputElement && !revealed) return;
      if (!revealed && event.key === "Enter") {
        event.preventDefault();
        handleCheck();
        return;
      }
      if (revealed && !showNearDialog) {
        const match = RATINGS.find((r) => r.shortcut === event.key);
        if (match) {
          event.preventDefault();
          handleRate(match.rating);
        }
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [revealed, grade, showNearDialog, pendingRating]);

  if (finished) {
    return <SessionSummary total={cards.length} stats={stats} />;
  }

  const total = cards.length;
  const progress = Math.round(((index) / total) * 100);

  return (
    <div className="flex flex-col gap-6 max-w-2xl mx-auto">
      <div>
        <div className="flex items-center justify-between text-sm text-muted-foreground mb-1">
          <span>Card {index + 1} of {total}</span>
          <span className="uppercase tracking-wide">{card.mode}</span>
        </div>
        <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
          <div
            className="h-full bg-primary transition-all"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <Card>
        <CardContent className="pt-8 pb-6 flex flex-col items-center gap-6">
          {card.mode === "RECOGNITION" ? (
            <RecognitionCard
              card={card}
              pinyin={pinyin}
              meaning={meaning}
              onPinyinChange={setPinyin}
              onMeaningChange={setMeaning}
              onSubmit={handleCheck}
              revealed={revealed}
              grade={grade}
              pinyinInputRef={pinyinInputRef}
            />
          ) : (
            <ProductionPlaceholder card={card} />
          )}
        </CardContent>
      </Card>

      {revealed ? (
        <RatingButtons
          suggested={grade?.suggestedRating ?? "GOOD"}
          onPick={handleRate}
          disabled={reviewMutation.isPending || showNearDialog}
        />
      ) : (
        <div className="flex justify-center">
          <Button size="lg" onClick={handleCheck} disabled={card.mode === "PRODUCTION"}>
            Check (Enter)
          </Button>
        </div>
      )}

      <NearMatchDialog
        open={showNearDialog}
        guess={meaning}
        suggestion={grade?.bestMeaning ?? null}
        onCancel={() => {
          setShowNearDialog(false);
          setPendingRating(null);
        }}
        onConfirm={() => {
          setShowNearDialog(false);
          if (pendingRating) {
            applyRating(pendingRating);
            setPendingRating(null);
          }
        }}
      />
    </div>
  );
}

function RecognitionCard({
  card,
  pinyin,
  meaning,
  onPinyinChange,
  onMeaningChange,
  onSubmit,
  revealed,
  grade,
  pinyinInputRef,
}: {
  card: StudyCard;
  pinyin: string;
  meaning: string;
  onPinyinChange: (value: string) => void;
  onMeaningChange: (value: string) => void;
  onSubmit: () => void;
  revealed: boolean;
  grade: GradeResult | null;
  pinyinInputRef: React.RefObject<HTMLInputElement>;
}) {
  return (
    <>
      <div
        className="text-7xl sm:text-8xl font-serif select-none"
        lang="zh-Hans"
        aria-label={`Hanzi character ${card.character ?? ""}`}
      >
        {card.character}
      </div>

      <form
        className="w-full flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          if (!revealed) onSubmit();
        }}
      >
        <div className="flex flex-col gap-1">
          <label htmlFor="pinyin" className="text-sm font-medium">Pinyin</label>
          <Input
            id="pinyin"
            ref={pinyinInputRef}
            value={pinyin}
            onChange={(event) => onPinyinChange(event.target.value)}
            disabled={revealed}
            autoComplete="off"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            placeholder="e.g. ni3 or nǐ"
          />
          {revealed ? (
            <p className={cn("text-xs", grade?.pinyinOk ? "text-emerald-600" : "text-destructive")}>
              {grade?.pinyinOk ? "Correct pinyin" : `Expected: ${card.pinyin}`}
            </p>
          ) : null}
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="meaning" className="text-sm font-medium">Meaning</label>
          <Input
            id="meaning"
            value={meaning}
            onChange={(event) => onMeaningChange(event.target.value)}
            disabled={revealed}
            autoComplete="off"
            placeholder="English meaning"
          />
          {revealed ? (
            <p
              className={cn(
                "text-xs",
                grade?.meaningOk
                  ? "text-emerald-600"
                  : grade?.outcome === "NEAR"
                    ? "text-amber-600"
                    : "text-destructive",
              )}
            >
              {grade?.meaningOk
                ? "Correct meaning"
                : `Accepted: ${(card.meanings ?? []).join(", ") || "—"}`}
            </p>
          ) : null}
        </div>
      </form>
    </>
  );
}

function ProductionPlaceholder({ card }: { card: StudyCard }) {
  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <div className="text-2xl font-medium">
        {(card.meanings ?? []).join(", ") || "—"}
      </div>
      <p className="text-sm text-muted-foreground">
        Drawing mode arrives in TASK-022. For now, self-grade after recalling the character.
      </p>
    </div>
  );
}

function RatingButtons({
  suggested,
  onPick,
  disabled,
}: {
  suggested: Rating;
  onPick: (rating: Rating) => void;
  disabled: boolean;
}) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
      {RATINGS.map((r) => (
        <Button
          key={r.rating}
          onClick={() => onPick(r.rating)}
          disabled={disabled}
          className={cn(
            "h-14 text-base font-semibold",
            r.className,
            suggested === r.rating && "ring-2 ring-offset-2 ring-foreground",
          )}
        >
          <span className="flex flex-col leading-tight">
            <span>{r.label}</span>
            <span className="text-[10px] opacity-80">[{r.shortcut}]</span>
          </span>
        </Button>
      ))}
    </div>
  );
}

function NearMatchDialog({
  open,
  guess,
  suggestion,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  guess: string;
  suggestion: string | null;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <Dialog open={open} onOpenChange={(value) => { if (!value) onCancel(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Did you mean &ldquo;{suggestion ?? "—"}&rdquo;?</DialogTitle>
          <DialogDescription>
            Your answer &ldquo;{guess}&rdquo; was close. Count it as correct?
          </DialogDescription>
        </DialogHeader>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="outline" onClick={onCancel}>No, mark wrong</Button>
          <Button onClick={onConfirm}>Yes, accept</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function SessionSkeleton() {
  return (
    <div className="max-w-2xl mx-auto flex flex-col gap-6">
      <div className="h-2 w-full rounded-full bg-muted animate-pulse" />
      <Card>
        <CardContent className="pt-8 pb-6 flex flex-col items-center gap-6">
          <div className="h-24 w-24 rounded bg-muted animate-pulse" />
          <div className="h-10 w-full rounded bg-muted animate-pulse" />
          <div className="h-10 w-full rounded bg-muted animate-pulse" />
        </CardContent>
      </Card>
    </div>
  );
}

function EmptyQueue() {
  return (
    <Card className="max-w-xl mx-auto">
      <CardHeader>
        <CardTitle>All caught up</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground">
          Nothing is due right now. Subscribe to a deck or come back later.
        </p>
        <Button asChild variant="outline">
          <Link to="/decks">Browse decks</Link>
        </Button>
      </CardContent>
    </Card>
  );
}

function SessionSummary({ total, stats }: { total: number; stats: SessionStat }) {
  const correctish = stats.good + stats.easy;
  const accuracy = total > 0 ? Math.round((correctish / total) * 100) : 0;
  return (
    <Card className="max-w-xl mx-auto">
      <CardHeader>
        <CardTitle>Session complete 🎉</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <p className="text-sm text-muted-foreground">
          You reviewed <strong>{total}</strong> card{total === 1 ? "" : "s"}.
        </p>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-center">
          <SummaryStat label="Again" value={stats.again} className="text-rose-600" />
          <SummaryStat label="Hard" value={stats.hard} className="text-amber-600" />
          <SummaryStat label="Good" value={stats.good} className="text-emerald-600" />
          <SummaryStat label="Easy" value={stats.easy} className="text-sky-600" />
        </div>
        <p className="text-sm">Accuracy this session: <strong>{accuracy}%</strong></p>
        <div className="flex gap-2">
          <Button asChild>
            <Link to="/">Back to dashboard</Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/study" reloadDocument>Start another</Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function SummaryStat({ label, value, className }: { label: string; value: number; className: string }) {
  return (
    <div className="rounded-md border p-3">
      <div className={cn("text-2xl font-semibold", className)}>{value}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </div>
  );
}
