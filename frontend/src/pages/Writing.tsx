import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { HanziDrawingPad, type DrawingResult } from "@/components/HanziDrawingPad";
import { BrushDivider } from "@/components/ui/brush-divider";
import { Seal } from "@/components/ui/seal";
import { Button } from "@/components/ui/button";
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
  type WritingLesson,
  type WritingText,
} from "@/lib/writing-texts";

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
          Pick a set of texts and copy each character stroke by stroke. The English
          translation and pinyin are shown as your prompt.
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

interface RunnerStats {
  written: number;
  skipped: number;
}

function LessonRunner({
  lesson,
  onExit,
}: {
  lesson: WritingLesson;
  onExit: () => void;
}) {
  const helpLevel = usePreferencesStore((s) => s.helpLevel);
  const totalChars = useMemo(() => countCharacters(lesson), [lesson]);

  const [textIndex, setTextIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [doneChars, setDoneChars] = useState(0);
  const [stats, setStats] = useState<RunnerStats>({ written: 0, skipped: 0 });

  const text = lesson.texts[textIndex];
  const chars = useMemo(() => (text ? Array.from(text.hanzi) : []), [text]);
  const finished = textIndex >= lesson.texts.length;

  function advance() {
    setDoneChars((n) => n + 1);
    const isLastChar = charIndex + 1 >= chars.length;
    if (!isLastChar) {
      setCharIndex((i) => i + 1);
      return;
    }
    setCharIndex(0);
    setTextIndex((i) => i + 1);
  }

  function handleComplete(_result: DrawingResult) {
    setStats((prev) => ({ ...prev, written: prev.written + 1 }));
    advance();
  }

  function handleSkip() {
    setStats((prev) => ({ ...prev, skipped: prev.skipped + 1 }));
    advance();
  }

  if (finished) {
    return (
      <WritingComplete lesson={lesson} stats={stats} onExit={onExit} />
    );
  }

  const progress = totalChars > 0 ? Math.round((doneChars / totalChars) * 100) : 0;

  return (
    <div className="flex flex-col gap-6 max-w-2xl mx-auto w-full min-w-0">
      <ProgressHeader
        title={lesson.title}
        current={doneChars + 1}
        total={totalChars}
        progress={progress}
        onExit={onExit}
      />

      <Card>
        <CardContent className="pt-8 pb-6 flex flex-col items-center gap-6 min-w-0">
          <TextPrompt text={text} activeCharIndex={charIndex} />
          <HanziDrawingPad
            key={`${textIndex}-${charIndex}`}
            character={chars[charIndex]}
            helpLevel={helpLevel}
            onComplete={handleComplete}
            onSkip={handleSkip}
          />
        </CardContent>
      </Card>
    </div>
  );
}

function ProgressHeader({
  title,
  current,
  total,
  progress,
  onExit,
}: {
  title: string;
  current: number;
  total: number;
  progress: number;
  onExit: () => void;
}) {
  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-x-3 text-sm text-muted-foreground mb-1">
        <span>
          {title} · character {Math.min(current, total)} of {total}
        </span>
        <Button variant="ghost" size="sm" onClick={onExit}>
          Exit
        </Button>
      </div>
      <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
        <div className="h-full bg-seal transition-all" style={{ width: `${progress}%` }} />
      </div>
    </div>
  );
}

function TextPrompt({
  text,
  activeCharIndex,
}: {
  text: WritingText;
  activeCharIndex: number;
}) {
  const chars = Array.from(text.hanzi);
  return (
    <div className="flex flex-col items-center gap-2 text-center">
      <div className="text-2xl font-medium text-ink">{text.english}</div>
      <div className="text-sm text-muted-foreground">{text.pinyin}</div>
      <div className="flex flex-wrap justify-center gap-1.5 mt-1" lang="zh-Hans" aria-hidden>
        {chars.map((char, index) => (
          <span
            key={`${char}-${index}`}
            className={cn(
              "font-hanzi text-2xl leading-none px-1.5 py-0.5 rounded transition-colors",
              index < activeCharIndex && "text-ink-soft",
              index === activeCharIndex && "bg-seal/15 text-seal ring-1 ring-seal/40",
              index > activeCharIndex && "text-ink-soft/50",
            )}
          >
            {char}
          </span>
        ))}
      </div>
      <div className="text-[10px] uppercase tracking-wider text-muted-foreground/70 mt-1">
        Write the highlighted character
      </div>
    </div>
  );
}

function WritingComplete({
  lesson,
  stats,
  onExit,
}: {
  lesson: WritingLesson;
  stats: RunnerStats;
  onExit: () => void;
}) {
  const total = stats.written + stats.skipped;
  const accuracy = total > 0 ? Math.round((stats.written / total) * 100) : 0;

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center gap-8 max-w-xl mx-auto text-center">
      <div className="flex flex-col items-center gap-3">
        <span className="text-xs uppercase tracking-[0.4em] text-ink-soft">
          Lesson complete
        </span>
        <Seal shape="round" size="lg" title="Writing practice complete">
          写
        </Seal>
        <div className="font-hanzi text-2xl text-ink">
          {lesson.title}
        </div>
      </div>

      <BrushDivider className="max-w-xs" />

      <p className="text-base text-ink-soft">
        You wrote <strong className="text-ink">{stats.written}</strong> character
        {stats.written === 1 ? "" : "s"}
        {stats.skipped > 0 ? (
          <>
            {" "}
            and skipped <strong className="text-ink">{stats.skipped}</strong>
          </>
        ) : null}
        {" "}— {accuracy}% written.
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
