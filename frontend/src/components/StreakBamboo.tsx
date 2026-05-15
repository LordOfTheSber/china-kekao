import { cn } from "@/lib/utils";

interface Props {
  streak: number;
  className?: string;
}

/**
 * Tiny growing-bamboo SVG. Renders one segment per streak day up to a cap,
 * with the latest segment animated in (path stroke-dashoffset).
 */
export function StreakBamboo({ streak, className }: Props) {
  const segments = Math.min(streak, 10);
  if (segments <= 0) return null;
  const segmentHeight = 14;
  const totalHeight = segments * segmentHeight + 8;
  return (
    <svg
      aria-hidden
      viewBox={`0 0 24 ${totalHeight}`}
      className={cn("h-20 w-6 text-success", className)}
    >
      {Array.from({ length: segments }).map((_, i) => {
        const y = totalHeight - (i + 1) * segmentHeight;
        const isLatest = i === segments - 1;
        return (
          <g key={i}>
            <rect
              x="9"
              y={y}
              width="6"
              height={segmentHeight - 2}
              rx="1.5"
              fill="currentColor"
              opacity={0.65 + i * 0.03}
              className={cn(
                isLatest && "origin-bottom",
                isLatest && "animate-in zoom-in-50 motion-reduce:animate-none",
              )}
            />
            <line
              x1="6"
              y1={y + segmentHeight - 2}
              x2="18"
              y2={y + segmentHeight - 2}
              stroke="hsl(var(--paper))"
              strokeWidth="1.5"
            />
          </g>
        );
      })}
      {/* Two leaves on top */}
      <path
        d={`M 12 ${totalHeight - segments * segmentHeight} q -6 -4 -8 -1 q 2 5 8 3 z`}
        fill="currentColor"
        opacity="0.85"
      />
      <path
        d={`M 12 ${totalHeight - segments * segmentHeight + 2} q 6 -4 9 0 q -2 5 -9 3 z`}
        fill="currentColor"
        opacity="0.75"
      />
    </svg>
  );
}
