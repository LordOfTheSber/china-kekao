import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { fetchDistractors, fetchStudySession, postReview } from "@/api/study";
import { extractErrorMessage } from "@/api/auth";
import type { Rating, StudyCard } from "@/api/types";
import { HanziDrawingPad, type DrawingResult } from "@/components/HanziDrawingPad";
import { HanziChoiceGrid } from "@/components/HanziChoiceGrid";
import { usePreferencesStore } from "@/store/preferences";
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
  const productionMode = usePreferencesStore((s) => s.productionMode);
  const helpLevel = usePreferencesStore((s) => s.helpLevel);
  const withTones = usePreferencesStore((s) => s.withTones);

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
  const isProduction = !!card && card.mode === "PRODUCTION";

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
    const result = gradeAnswer(card.pinyin, acceptedMeanings, pinyin, meaning, { withTones });
    setGrade(result);
    setRevealed(true);
  }

  function applyRating(
    rating: Rating,
    extras?: { hintCount?: number; strokeMistakes?: number; responseTimeMs?: number },
  ) {
    if (!card) return;
    const responseTimeMs =
      extras?.responseTimeMs ??
      Math.max(0, Math.round(performance.now() - startedAtRef.current));
    reviewMutation.mutate({
      userCardId: card.userCardId,
      mode: card.mode,
      rating,
      responseTimeMs,
      hintCount: extras?.hintCount ?? 0,
      strokeMistakes: extras?.strokeMistakes ?? 0,
    });
    setStats((prev) => ({
      again: prev.again + (rating === "AGAIN" ? 1 : 0),
      hard: prev.hard + (rating === "HARD" ? 1 : 0),
      good: prev.good + (rating === "GOOD" ? 1 : 0),
      easy: prev.easy + (rating === "EASY" ? 1 : 0),
    }));
    setIndex((i) => i + 1);
  }

  function handleDrawingComplete(result: DrawingResult) {
    applyRating(result.rating, {
      hintCount: result.hintCount,
      strokeMistakes: result.strokeMistakes,
      responseTimeMs: result.durationMs,
    });
  }

  function handleDrawingSkip() {
    applyRating("AGAIN");
  }

  function handleChoiceComplete(rating: Rating) {
    applyRating(rating);
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
          ) : productionMode === "DRAWING" ? (
            <ProductionDrawing
              key={card.userCardId}
              card={card}
              helpLevel={helpLevel}
              onComplete={handleDrawingComplete}
              onSkip={handleDrawingSkip}
            />
          ) : (
            <ProductionChoice
              key={card.userCardId}
              card={card}
              onComplete={handleChoiceComplete}
            />
          )}
        </CardContent>
      </Card>

      {isProduction ? null : revealed ? (
        <RatingButtons
          suggested={grade?.suggestedRating ?? "GOOD"}
          onPick={handleRate}
          disabled={reviewMutation.isPending || showNearDialog}
        />
      ) : (
        <div className="flex justify-center">
          <Button size="lg" onClick={handleCheck}>
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

function ProductionPrompt({ card }: { card: StudyCard }) {
  return (
    <div className="flex flex-col items-center gap-1 text-center">
      <div className="text-2xl font-medium">
        {(card.meanings ?? []).join(", ") || "—"}
      </div>
      <div className="text-xs uppercase tracking-wide text-muted-foreground">
        {card.pinyin}
      </div>
    </div>
  );
}

function ProductionDrawing({
  card,
  helpLevel,
  onComplete,
  onSkip,
}: {
  card: StudyCard;
  helpLevel: import("@/store/preferences").HelpLevel;
  onComplete: (result: DrawingResult) => void;
  onSkip: () => void;
}) {
  return (
    <div className="flex flex-col items-center gap-6 w-full">
      <ProductionPrompt card={card} />
      {card.character ? (
        <HanziDrawingPad
          character={card.character}
          helpLevel={helpLevel}
          onComplete={onComplete}
          onSkip={onSkip}
        />
      ) : (
        <ProductionDrawingMissing onSkip={onSkip} />
      )}
    </div>
  );
}

function ProductionDrawingMissing({ onSkip }: { onSkip: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 text-center">
      <p className="text-sm text-destructive">
        This card is missing the target character. Skipping is the only option.
      </p>
      <Button variant="outline" size="sm" onClick={onSkip}>
        Skip
      </Button>
    </div>
  );
}

function ProductionChoice({
  card,
  onComplete,
}: {
  card: StudyCard;
  onComplete: (rating: Rating) => void;
}) {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["distractors", card.hanziId],
    queryFn: () => fetchDistractors(card.hanziId, 5),
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center gap-4 w-full">
        <ProductionPrompt card={card} />
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 w-full">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-20 rounded-md bg-muted animate-pulse" />
          ))}
        </div>
      </div>
    );
  }
  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3">
        <p className="text-sm text-destructive">Could not load choices.</p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
      </div>
    );
  }
  if (!card.character) {
    return (
      <div className="flex flex-col items-center gap-3 text-center">
        <p className="text-sm text-destructive">Missing target character.</p>
        <Button variant="outline" size="sm" onClick={() => onComplete("AGAIN")}>
          Skip
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-4 w-full">
      <ProductionPrompt card={card} />
      <HanziChoiceGrid
        correctCharacter={card.character}
        distractors={data.distractors}
        onComplete={onComplete}
      />
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
