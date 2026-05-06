import { useEffect, useMemo, useState } from "react";

import { Button } from "@/components/ui/button";
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

  useEffect(() => {
    setPicked(null);
  }, [correctCharacter]);

  function handlePick(value: string) {
    if (picked) return;
    setPicked(value);
    const rating: Rating = value === correctCharacter ? "GOOD" : "AGAIN";
    setTimeout(() => onComplete(rating), 450);
  }

  return (
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
              "h-20 text-3xl font-serif",
              reveal && isCorrect && "bg-emerald-100 border-emerald-500 text-emerald-900",
              reveal && isPicked && !isCorrect && "bg-rose-100 border-rose-500 text-rose-900",
            )}
            lang="zh-Hans"
          >
            {choice}
          </Button>
        );
      })}
    </div>
  );
}
