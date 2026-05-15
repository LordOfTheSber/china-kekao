import { useEffect, useRef, useState } from "react";
import HanziWriter from "hanzi-writer";

import { Button } from "@/components/ui/button";
import type { Rating } from "@/api/types";
import type { HelpLevel } from "@/store/preferences";

export interface DrawingResult {
  rating: Rating;
  hintCount: number;
  strokeMistakes: number;
  durationMs: number;
}

export interface HanziDrawingPadProps {
  character: string;
  helpLevel: HelpLevel;
  onComplete: (result: DrawingResult) => void;
  onSkip: () => void;
}

interface QuizOptions {
  leniency: number;
  showHintAfterMisses: number;
  highlightOnComplete: boolean;
}

function quizOptionsFor(helpLevel: HelpLevel): QuizOptions {
  switch (helpLevel) {
    case "STRICT":
      return { leniency: 1.0, showHintAfterMisses: 999, highlightOnComplete: false };
    case "EASY":
      return { leniency: 1.5, showHintAfterMisses: 1, highlightOnComplete: true };
    case "NORMAL":
    default:
      return { leniency: 1.0, showHintAfterMisses: 3, highlightOnComplete: true };
  }
}

function ratingFor(hintCount: number, strokeMistakes: number, completed: boolean): Rating {
  if (!completed) return "AGAIN";
  if (hintCount === 0 && strokeMistakes === 0) return "EASY";
  if (hintCount <= 2 && strokeMistakes <= 2) return "GOOD";
  return "HARD";
}

function pickSize(): number {
  if (typeof window === "undefined") return 320;
  const available = Math.min(window.innerWidth - 48, window.innerHeight - 320);
  return Math.max(280, Math.min(400, available));
}

export function HanziDrawingPad({
  character,
  helpLevel,
  onComplete,
  onSkip,
}: HanziDrawingPadProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const writerRef = useRef<ReturnType<typeof HanziWriter.create> | null>(null);
  const startedAtRef = useRef<number>(performance.now());
  const hintCountRef = useRef<number>(0);
  const mistakeCountRef = useRef<number>(0);
  const lastMistakeStrokeRef = useRef<number>(-1);
  const reportedRef = useRef<boolean>(false);

  const [size, setSize] = useState<number>(() => pickSize());
  const [loadError, setLoadError] = useState<string | null>(null);
  const [animating, setAnimating] = useState<boolean>(false);

  useEffect(() => {
    function onResize() {
      setSize(pickSize());
    }
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => {
    if (!containerRef.current) return;
    containerRef.current.innerHTML = "";

    startedAtRef.current = performance.now();
    hintCountRef.current = 0;
    mistakeCountRef.current = 0;
    lastMistakeStrokeRef.current = -1;
    reportedRef.current = false;
    setLoadError(null);

    const opts = quizOptionsFor(helpLevel);

    let cancelled = false;
    let writer: ReturnType<typeof HanziWriter.create> | null = null;

    try {
      writer = HanziWriter.create(containerRef.current, character, {
        width: size,
        height: size,
        padding: 8,
        showCharacter: false,
        showOutline: helpLevel !== "STRICT",
        strokeAnimationSpeed: 1,
        delayBetweenStrokes: 80,
        onLoadCharDataError: (err: unknown) => {
          if (cancelled) return;
          setLoadError(err instanceof Error ? err.message : String(err));
        },
      });
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : String(err));
      return;
    }

    writerRef.current = writer;

    writer.quiz({
      leniency: opts.leniency,
      showHintAfterMisses: opts.showHintAfterMisses,
      highlightOnComplete: opts.highlightOnComplete,
      onMistake: (info: { strokeNum: number; mistakesOnStroke: number; totalMistakes: number }) => {
        if (cancelled) return;
        mistakeCountRef.current = info.totalMistakes;
        if (
          opts.showHintAfterMisses < 999 &&
          info.mistakesOnStroke >= opts.showHintAfterMisses &&
          lastMistakeStrokeRef.current !== info.strokeNum
        ) {
          lastMistakeStrokeRef.current = info.strokeNum;
          hintCountRef.current += 1;
        }
      },
      onComplete: (info: { totalMistakes: number }) => {
        if (cancelled || reportedRef.current) return;
        reportedRef.current = true;
        const durationMs = Math.max(0, Math.round(performance.now() - startedAtRef.current));
        onComplete({
          rating: ratingFor(hintCountRef.current, info.totalMistakes, true),
          hintCount: hintCountRef.current,
          strokeMistakes: info.totalMistakes,
          durationMs,
        });
      },
    });

    return () => {
      cancelled = true;
      writerRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [character, helpLevel, size]);

  function handleSkip() {
    if (reportedRef.current) return;
    reportedRef.current = true;
    onSkip();
  }

  function handleShowOrder() {
    const writer = writerRef.current;
    if (!writer || animating) return;
    setAnimating(true);
    hintCountRef.current += 1;
    writer
      .animateCharacter()
      ?.then?.(() => setAnimating(false))
      ?.catch?.(() => setAnimating(false));
    setTimeout(() => setAnimating(false), 8000);
  }

  return (
    <div className="flex flex-col items-center gap-4 w-full min-w-0">
      {loadError ? (
        <div className="text-sm text-destructive">
          Could not load stroke data for &ldquo;{character}&rdquo;.
        </div>
      ) : null}
      <div
        ref={containerRef}
        className="rounded-lg border bg-background shadow-card max-w-full"
        style={{
          width: size,
          height: size,
          touchAction: "none",
        }}
      />
      <div className="flex flex-wrap justify-center gap-2">
        <Button variant="outline" size="sm" onClick={handleSkip}>
          Skip
        </Button>
        <Button variant="outline" size="sm" onClick={handleShowOrder} disabled={animating}>
          Show stroke order
        </Button>
      </div>
    </div>
  );
}
