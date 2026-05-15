import { useEffect } from "react";

import { cn } from "@/lib/utils";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function PandaEgg({ open, onClose }: Props) {
  useEffect(() => {
    if (!open) return;
    const t = window.setTimeout(onClose, 4200);
    return () => window.clearTimeout(t);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <button
      onClick={onClose}
      className={cn(
        "fixed inset-0 z-50 flex items-center justify-center bg-paper/85 backdrop-blur",
        "animate-in fade-in-0 motion-reduce:animate-none",
      )}
      aria-label="Hidden panda — click to dismiss"
    >
      <div className="flex flex-col items-center gap-4">
        <div
          className="text-[8rem] select-none animate-ink-pulse motion-reduce:animate-none"
          aria-hidden
        >
          🐼
        </div>
        <div className="font-hanzi text-3xl text-ink">熊猫</div>
        <div className="text-sm text-ink-soft uppercase tracking-[0.3em]">
          xióngmāo · panda
        </div>
        <div className="text-xs text-ink-soft mt-2">
          (click anywhere to dismiss)
        </div>
      </div>
    </button>
  );
}
