import { cn } from "@/lib/utils";

type BrushDividerProps = {
  className?: string;
  variant?: "horizontal" | "vertical";
};

export function BrushDivider({
  className,
  variant = "horizontal",
}: BrushDividerProps) {
  if (variant === "vertical") {
    return (
      <svg
        aria-hidden
        viewBox="0 0 8 200"
        preserveAspectRatio="none"
        className={cn("h-full w-2 text-brush", className)}
      >
        <path
          d="M 4 2 C 5 30, 3 60, 4 100 C 5 140, 3 170, 4 198"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          fill="none"
          opacity="0.55"
        />
      </svg>
    );
  }
  return (
    <svg
      aria-hidden
      viewBox="0 0 600 12"
      preserveAspectRatio="none"
      className={cn("h-3 w-full text-brush", className)}
    >
      <path
        d="M 4 6 C 80 3, 160 9, 240 5 C 320 2, 400 10, 480 6 C 540 4, 580 8, 596 6"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        fill="none"
        opacity="0.55"
      />
      <path
        d="M 30 7 C 100 6, 200 8, 300 6 C 400 5, 500 7, 570 6"
        stroke="currentColor"
        strokeWidth="0.9"
        strokeLinecap="round"
        fill="none"
        opacity="0.35"
      />
    </svg>
  );
}
