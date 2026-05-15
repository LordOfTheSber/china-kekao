import { animate as motionAnimate } from "motion";

import { useThemeStore } from "@/store/theme";

export function isMotionReduced(): boolean {
  const pref = useThemeStore.getState().motion;
  if (pref === "off") return true;
  if (pref === "on") return false;
  if (typeof window === "undefined" || !window.matchMedia) return false;
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

type AnimateArgs = Parameters<typeof motionAnimate>;

export function animate(...args: AnimateArgs): ReturnType<typeof motionAnimate> | undefined {
  if (isMotionReduced()) {
    return undefined;
  }
  return motionAnimate(...args);
}
