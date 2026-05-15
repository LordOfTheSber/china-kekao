import { useEffect, useState } from "react";

import { Seal } from "@/components/ui/seal";
import { cn } from "@/lib/utils";

export type FeedbackKind = "correct" | "wrong" | null;

interface Props {
  trigger: FeedbackKind;
  onDone?: () => void;
  className?: string;
}

/**
 * Visual "对" seal overlay on correct answers. The wrong-answer wobble is
 * applied directly to the target via the `gentle-wobble` Tailwind animation
 * (see tailwind.config.js).
 */
export function FeedbackSeal({ trigger, onDone, className }: Props) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (trigger === "correct") {
      setVisible(true);
      const t = window.setTimeout(() => {
        setVisible(false);
        onDone?.();
      }, 700);
      return () => window.clearTimeout(t);
    }
    return undefined;
  }, [trigger, onDone]);

  if (!visible) return null;

  return (
    <div
      className={cn(
        "pointer-events-none absolute inset-0 z-20 flex items-center justify-center",
        className,
      )}
      aria-hidden
    >
      <span className="animate-seal-pop">
        <Seal shape="round" size="lg" tilt={false}>
          对
        </Seal>
      </span>
    </div>
  );
}
