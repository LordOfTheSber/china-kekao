import { useEffect, useMemo, useState } from "react";

import { Button } from "@/components/ui/button";
import { FeedbackSeal, type FeedbackKind } from "@/components/FeedbackOverlay";
import { cn } from "@/lib/utils";
import type { Rating } from "@/api/types";

export interface HanziChoiceGridProps {
  correctCharacter: string;
  distractors: string[];
  onComplete: (rating: Rating) => void;
}

function shuffle<T>(items: T[]): T[] {
  const arr = items.slice();
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

export function HanziChoiceGrid({
  correctCharacter,
  distractors,
  onComplete,
}: HanziChoiceGridProps) {
  const choices = useMemo(
    () => shuffle([correctCharacter, ...distractors.slice(0, 5)]),
    [correctCharacter, distractors],
  );
  const [picked, setPicked] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<FeedbackKind>(null);

  useEffect(() => {
    setPicked(null);
    setFeedback(null);
  }, [correctCharacter]);

  function handlePick(value: string) {
    if (picked) return;
    setPicked(value);
    const correct = value === correctCharacter;
    setFeedback(correct ? "correct" : "wrong");
    const rating: Rating = correct ? "GOOD" : "AGAIN";
    setTimeout(() => onComplete(rating), correct ? 700 : 600);
  }

  return (
    <div className="relative w-full">
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 w-full">
        {choices.map((choice) => {
          const isPicked = picked === choice;
          const isCorrect = choice === correctCharacter;
          const reveal = picked !== null;
          return (
            <Button
              key={choice}
              type="button"
              variant="outline"
              disabled={picked !== null}
              onClick={() => handlePick(choice)}
              className={cn(
                "aspect-square h-auto w-full text-3xl sm:text-4xl font-hanzi min-w-0 transition-all",
                reveal && isCorrect &&
                  "bg-success/15 border-success text-success",
                reveal && isPicked && !isCorrect &&
                  "bg-destructive/10 border-destructive text-destructive animate-gentle-wobble",
              )}
              lang="zh-Hans"
            >
              {choice}
            </Button>
          );
        })}
      </div>
      <FeedbackSeal trigger={feedback} />
    </div>
  );
}
