import * as React from "react";

import { cn } from "@/lib/utils";

type SealProps = {
  children: React.ReactNode;
  shape?: "square" | "round";
  size?: "sm" | "md" | "lg";
  className?: string;
  tilt?: boolean;
  title?: string;
};

const SIZE_CLASSES: Record<NonNullable<SealProps["size"]>, string> = {
  sm: "h-7 min-w-7 text-xs px-1.5",
  md: "h-10 min-w-10 text-base px-2",
  lg: "h-14 min-w-14 text-2xl px-3",
};

export function Seal({
  children,
  shape = "square",
  size = "md",
  className,
  tilt = true,
  title,
}: SealProps) {
  return (
    <span
      title={title}
      aria-label={title}
      className={cn(
        "inline-flex select-none items-center justify-center bg-seal text-seal-foreground font-hanzi font-bold leading-none shadow-seal",
        shape === "round" ? "rounded-full" : "rounded-[3px]",
        tilt && "-rotate-3",
        SIZE_CLASSES[size],
        className,
      )}
      style={{
        // Slight ink-bleed edge so the stamp doesn't look like a sticker.
        boxShadow:
          "inset 0 0 0 1px hsl(var(--seal) / 0.85), 0 2px 0 hsl(var(--seal) / 0.18), 0 6px 18px -6px hsl(var(--seal) / 0.45)",
      }}
    >
      {children}
    </span>
  );
}
