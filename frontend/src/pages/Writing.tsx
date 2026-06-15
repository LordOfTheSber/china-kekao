import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { HanziDrawingPad, type DrawingResult } from "@/components/HanziDrawingPad";
import { BrushDivider } from "@/components/ui/brush-divider";
import { Seal } from "@/components/ui/seal";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { usePreferencesStore } from "@/store/preferences";
import { cn } from "@/lib/utils";
import {
  WRITING_LESSONS,
  countCharacters,
  isWritable,
  writableChars,
  type WritingLesson,
  type WritingText,
} from "@/lib/writing-texts";

type InputMethod = "DRAW" | "TYPE";

interface RunnerStats {
  correct: number;
  missed: number;
}

export function WritingPage() {
  const [lesson, setLesson] = useState<WritingLesson | null>(null);

  if (!lesson) {
    return <LessonPicker onPick={setLesson} />;
  }
  return (
    <LessonRunner
      key={lesson.id}
      lesson={lesson}
      onExit={() => setLesson(null)}
    />
  );
}

function LessonPicker({ onPick }: { onPick: (lesson: WritingLesson) => void }) {
  return (
    <div className="flex flex-col gap-6 max-w-3xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold text-ink">Writing practice</h1>
        <p className="text-sm text-ink-soft">
          Pick a set of texts — from everyday phrases to lines from classic works like
          Romance of the Three Kingdoms — and reproduce each one. Trace it stroke by
          stroke, or type the characters from your keyboard. The English translation and
          pinyin are always shown as your prompt.
        </p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        {WRITING_LESSONS.map((lesson) => (
          <LessonCard key={lesson.id} lesson={lesson} onPick={onPick} />
        ))}
      </div>
    </div>
  );
}

function LessonCard({
  lesson,
  onPick,
}: {
  lesson: WritingLesson;
  onPick: (lesson: WritingLesson) => void;
}) {
  return (
    <Card className="flex flex-col">
      <CardHeader>
        <div className="flex items-start justify-between gap-3">
          <CardTitle className="text-lg">{lesson.title}</CardTitle>
          <span className="shrink-0 text-[10px] uppercase tracking-wider rounded-full bg-muted px-2 py-0.5 text-muted-foreground">
            HSK {lesson.level}
          </span>
        </div>
        <CardDescription>{lesson.description}</CardDescription>
      </CardHeader>
      <CardContent className="mt-auto flex items-center justify-between gap-3">
        <div className="flex flex-wrap gap-1 font-hanzi text-xl text-ink" lang="zh-Hans">
          {lesson.texts.slice(0, 4).map((text) => (
            <span key={text.hanzi}>{text.hanzi}</span>
          ))}
          {lesson.texts.length > 4 ? (
            <span className="text-ink-soft text-base self-end">…</span>
          ) : null}
        </div>
        <Button size="sm" onClick={() => onPick(lesson)}>
          Start
        </Button>
      </CardContent>
    </Card>
  );
}

function LessonRunner({
  lesson,
  onExit,
}: {
  lesson: WritingLesson;
  onExit: () => void;
}) {
  const [method, setMethod] = useState<InputMethod>("DRAW");

  if (method === "TYPE") {
    return (
      <TypeRunner
        key="type"
        lesson={lesson}
        method={method}
        onMethodChange={setMethod}
        onExit={onExit}
      />
    );
  }
  return (
    <DrawRunner
      key="draw"
      lesson={lesson}
      method={method}
      onMethodChange={setMethod}
      onExit={onExit}
    />
  );
}

function DrawRunner({
  lesson,
  method,
  onMethodChange,
  onExit,
}: {
  lesson: WritingLesson;
  method: InputMethod;
  onMethodChange: (method: InputMethod) => void;
  onExit: () => void;
}) {
  const helpLevel = usePreferencesStore((s) => s.helpLevel);
  const totalChars = useMemo(() => countCharacters(lesson), [lesson]);

  const [textIndex, setTextIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [doneChars, setDoneChars] = useState(0);
  const [stats, setStats] = useState<RunnerStats>({ correct: 0, missed: 0 });

  const text = lesson.texts[textIndex];
  const chars = useMemo(() => (text ? writableChars(text.hanzi) : []), [text]);
  const finished = textIndex >= lesson.texts.length;

  function advance() {
    setDoneChars((n) => n + 1);
    if (charIndex + 1 < chars.length) {
      setCharIndex((i) => i + 1);
      return;
    }
    setCharIndex(0);
    setTextIndex((i) => i + 1);
  }

  function handleComplete(_result: DrawingResult) {
    setStats((prev) => ({ ...prev, correct: prev.correct + 1 }));
    advance();
  }

  function handleSkip() {
    setStats((prev) => ({ ...prev, missed: prev.missed + 1 }));
    advance();
  }

  if (finished) {
    return (
      <WritingComplete
        lesson={lesson}
        stats={stats}
        unit="character"
        missedLabel="skipped"
        onExit={onExit}
      />
    );
  }

  const progress = totalChars > 0 ? Math.round((doneChars / totalChars) * 100) : 0;

  return (
    <RunnerShell
      lesson={lesson}
      method={method}
      onMethodChange={onMethodChange}
      onExit={onExit}
      counterLabel={`character ${Math.min(doneChars + 1, totalChars)} of ${totalChars}`}
      progress={progress}
    >
      <TextPrompt text={text} activeCharIndex={charIndex} />
      <HanziDrawingPad
        key={`${textIndex}-${charIndex}`}
        character={chars[charIndex]}
        helpLevel={helpLevel}
        onComplete={handleComplete}
        onSkip={handleSkip}
      />
    </RunnerShell>
  );
}

function normalize(value: string): string {
  // Compare on CJK glyphs only, so spaces and punctuation are optional for the learner.
  return Array.from(value).filter(isWritable).join("");
}

function TypeRunner({
  lesson,
  method,
  onMethodChange,
  onExit,
}: {
  lesson: WritingLesson;
  method: InputMethod;
  onMethodChange: (method: InputMethod) => void;
  onExit: () => void;
}) {
  const total = lesson.texts.length;
  const [textIndex, setTextIndex] = useState(0);
  const [value, setValue] = useState("");
  const [status, setStatus] = useState<"INPUT" | "CORRECT" | "WRONG">("INPUT");
  const [hadMistake, setHadMistake] = useState(false);
  const [stats, setStats] = useState<RunnerStats>({ correct: 0, missed: 0 });
  const inputRef = useRef<HTMLInputElement>(null);

  const text = lesson.texts[textIndex];
  const finished = textIndex >= total;

  useEffect(() => {
    setValue("");
    setStatus("INPUT");
    setHadMistake(false);
    inputRef.current?.focus();
  }, [textIndex]);

  function advance(scoredCorrect: boolean) {
    setStats((prev) => ({
      correct: prev.correct + (scoredCorrect ? 1 : 0),
      missed: prev.missed + (scoredCorrect ? 0 : 1),
    }));
    setTextIndex((i) => i + 1);
  }

  function handleCheck() {
    if (!text || status === "CORRECT") return;
    if (normalize(value) === normalize(text.hanzi)) {
      setStatus("CORRECT");
      window.setTimeout(() => advance(!hadMistake), 700);
    } else {
      setStatus("WRONG");
      setHadMistake(true);
    }
  }

  function handleRetry() {
    setValue("");
    setStatus("INPUT");
    inputRef.current?.focus();
  }

  if (finished) {
    return (
      <WritingComplete
        lesson={lesson}
        stats={stats}
        unit="text"
        missedLabel="missed"
        onExit={onExit}
      />
    );
  }

  const progress = total > 0 ? Math.round((textIndex / total) * 100) : 0;

  return (
    <RunnerShell
      lesson={lesson}
      method={method}
      onMethodChange={onMethodChange}
      onExit={onExit}
      counterLabel={`text ${textIndex + 1} of ${total}`}
      progress={progress}
    >
      <TextPrompt text={text} hideHanzi={status !== "WRONG"} />
      <TypeAnswer
        value={value}
        status={status}
        expected={text.hanzi}
        inputRef={inputRef}
        onChange={setValue}
        onCheck={handleCheck}
        onRetry={handleRetry}
        onSkip={() => advance(false)}
      />
    </RunnerShell>
  );
}

function TypeAnswer({
  value,
  status,
  expected,
  inputRef,
  onChange,
  onCheck,
  onRetry,
  onSkip,
}: {
  value: string;
  status: "INPUT" | "CORRECT" | "WRONG";
  expected: string;
  inputRef: React.RefObject<HTMLInputElement>;
  onChange: (value: string) => void;
  onCheck: () => void;
  onRetry: () => void;
  onSkip: () => void;
}) {
  return (
    <form
      className="w-full max-w-md flex flex-col items-center gap-3"
      onSubmit={(event) => {
        event.preventDefault();
        if (status !== "CORRECT") onCheck();
      }}
    >
      <Input
        ref={inputRef}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={status === "CORRECT"}
        lang="zh-Hans"
        autoComplete="off"
        autoCorrect="off"
        spellCheck={false}
        placeholder="Type the characters"
        className={cn(
          "text-center font-hanzi text-2xl h-14",
          status === "CORRECT" && "border-success text-success focus-visible:ring-success",
          status === "WRONG" && "border-destructive text-destructive focus-visible:ring-destructive",
        )}
      />
      {status === "WRONG" ? (
        <p className="text-sm text-destructive">
          Not quite — the answer is{" "}
          <span className="font-hanzi font-semibold" lang="zh-Hans">{expected}</span>
        </p>
      ) : null}
      {status === "CORRECT" ? (
        <p className="text-sm text-success">Correct!</p>
      ) : null}

      {status === "WRONG" ? (
        <div className="flex gap-2">
          <Button type="button" variant="outline" onClick={onRetry}>
            Try again
          </Button>
          <Button type="button" onClick={onSkip}>
            Next
          </Button>
        </div>
      ) : (
        <Button type="submit" size="lg" disabled={status === "CORRECT"}>
          Check (Enter)
        </Button>
      )}
    </form>
  );
}

function RunnerShell({
  lesson,
  method,
  onMethodChange,
  onExit,
  counterLabel,
  progress,
  children,
}: {
  lesson: WritingLesson;
  method: InputMethod;
  onMethodChange: (method: InputMethod) => void;
  onExit: () => void;
  counterLabel: string;
  progress: number;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-6 max-w-2xl mx-auto w-full min-w-0">
      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span className="text-sm text-muted-foreground">
            {lesson.title} · {counterLabel}
          </span>
          <div className="flex items-center gap-2">
            <MethodToggle method={method} onChange={onMethodChange} />
            <Button variant="ghost" size="sm" onClick={onExit}>
              Exit
            </Button>
          </div>
        </div>
        <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
          <div className="h-full bg-seal transition-all" style={{ width: `${progress}%` }} />
        </div>
      </div>

      <Card>
        <CardContent className="pt-8 pb-6 flex flex-col items-center gap-6 min-w-0">
          {children}
        </CardContent>
      </Card>
    </div>
  );
}

function MethodToggle({
  method,
  onChange,
}: {
  method: InputMethod;
  onChange: (method: InputMethod) => void;
}) {
  const options: Array<{ value: InputMethod; label: string }> = [
    { value: "DRAW", label: "Draw" },
    { value: "TYPE", label: "Type" },
  ];
  return (
    <div className="inline-flex rounded-full border border-brush/40 bg-muted p-0.5">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => onChange(option.value)}
          className={cn(
            "px-3 py-1 text-xs font-medium rounded-full transition-colors",
            method === option.value
              ? "bg-seal text-seal-foreground shadow-seal"
              : "text-ink-soft hover:text-ink",
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

interface PromptCell {
  char: string;
  /** Index among writable glyphs, or null for punctuation/spaces. */
  writableIndex: number | null;
}

function toPromptCells(hanzi: string): PromptCell[] {
  let counter = 0;
  return Array.from(hanzi).map((char) => ({
    char,
    writableIndex: isWritable(char) ? counter++ : null,
  }));
}

function TextPrompt({
  text,
  activeCharIndex,
  hideHanzi = false,
}: {
  text: WritingText;
  activeCharIndex?: number;
  hideHanzi?: boolean;
}) {
  const cells = useMemo(() => toPromptCells(text.hanzi), [text]);
  return (
    <div className="flex flex-col items-center gap-2 text-center">
      <div className="text-2xl font-medium text-ink">{text.english}</div>
      <div className="text-sm text-muted-foreground">{text.pinyin}</div>
      {hideHanzi ? null : (
        <div className="flex flex-wrap justify-center gap-1 mt-1" lang="zh-Hans" aria-hidden>
          {cells.map((cell, index) => (
            <span
              key={`${cell.char}-${index}`}
              className={cn(
                "font-hanzi text-2xl leading-none px-1 py-0.5 rounded transition-colors",
                cellToneClass(cell, activeCharIndex),
              )}
            >
              {cell.char}
            </span>
          ))}
        </div>
      )}
      <div className="text-[10px] uppercase tracking-wider text-muted-foreground/70 mt-1">
        {activeCharIndex !== undefined
          ? "Write the highlighted character"
          : "Type the full text for this prompt"}
      </div>
    </div>
  );
}

function cellToneClass(cell: PromptCell, activeCharIndex?: number): string {
  if (cell.writableIndex === null) return "text-ink-soft/40";
  if (activeCharIndex === undefined) return "text-ink";
  if (cell.writableIndex === activeCharIndex) return "bg-seal/15 text-seal ring-1 ring-seal/40";
  if (cell.writableIndex < activeCharIndex) return "text-ink-soft";
  return "text-ink-soft/50";
}

function WritingComplete({
  lesson,
  stats,
  unit,
  missedLabel,
  onExit,
}: {
  lesson: WritingLesson;
  stats: RunnerStats;
  unit: "character" | "text";
  missedLabel: string;
  onExit: () => void;
}) {
  const total = stats.correct + stats.missed;
  const accuracy = total > 0 ? Math.round((stats.correct / total) * 100) : 0;

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center gap-8 max-w-xl mx-auto text-center">
      <div className="flex flex-col items-center gap-3">
        <span className="text-xs uppercase tracking-[0.4em] text-ink-soft">
          Lesson complete
        </span>
        <Seal shape="round" size="lg" title="Writing practice complete">
          写
        </Seal>
        <div className="font-hanzi text-2xl text-ink">{lesson.title}</div>
      </div>

      <BrushDivider className="max-w-xs" />

      <p className="text-base text-ink-soft">
        You completed <strong className="text-ink">{stats.correct}</strong> {unit}
        {stats.correct === 1 ? "" : "s"}
        {stats.missed > 0 ? (
          <>
            {" "}
            and {missedLabel} <strong className="text-ink">{stats.missed}</strong>
          </>
        ) : null}
        {" "}— {accuracy}% correct.
      </p>

      <div className="flex flex-wrap justify-center gap-2 pt-2">
        <Button size="lg" onClick={onExit}>
          Back to lessons
        </Button>
        <Button asChild variant="outline" size="lg">
          <Link to="/">Dashboard</Link>
        </Button>
      </div>
    </div>
  );
}
