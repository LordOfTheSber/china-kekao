import { useEffect, useRef } from "react";

import { claimAchievement, type AchievementView } from "@/api/achievements";

const KONAMI_SEQ = [
  "ArrowUp",
  "ArrowUp",
  "ArrowDown",
  "ArrowDown",
  "ArrowLeft",
  "ArrowRight",
  "ArrowLeft",
  "ArrowRight",
  "b",
  "a",
];

/**
 * Listen for the Konami code globally. When matched: unlock the theme and
 * claim the KONAMI achievement.
 */
export function useKonamiCode(onMatch: (a: AchievementView | null) => void) {
  const idxRef = useRef(0);
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.target instanceof HTMLInputElement) {
        idxRef.current = 0;
        return;
      }
      const expected = KONAMI_SEQ[idxRef.current];
      // Compare case-insensitively for letter keys.
      const matched =
        e.key === expected || e.key.toLowerCase() === expected.toLowerCase();
      if (matched) {
        idxRef.current += 1;
        if (idxRef.current === KONAMI_SEQ.length) {
          idxRef.current = 0;
          claimAchievement("KONAMI")
            .then((r) =>
              onMatch(
                r.unlocked
                  ? (r as unknown as { achievement: AchievementView }).achievement
                  : null,
              ),
            )
            .catch(() => onMatch(null));
        }
      } else {
        idxRef.current = 0;
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onMatch]);
}
