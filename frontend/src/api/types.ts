import type { AchievementView } from "@/api/achievements";

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface ApiError {
  status: number;
  code?: string;
  message: string;
}

export interface DashboardStats {
  dueTodayCount: number;
  newAvailableCount: number;
  learnedTotal: number;
  currentStreak: number;
  accuracy7d: number;
}

export type StudyMode = "RECOGNITION" | "PRODUCTION";
export type Rating = "AGAIN" | "HARD" | "GOOD" | "EASY";
export type CardState = "NEW" | "LEARNING" | "REVIEW" | "RELEARNING";

export interface StudyCard {
  userCardId: number;
  hanziId: number;
  character: string | null;
  pinyin: string;
  mode: StudyMode;
  meanings: string[] | null;
  strokeData: string | null;
}

export interface ReviewRequest {
  userCardId: number;
  mode: StudyMode;
  rating: Rating;
  responseTimeMs: number;
  hintCount?: number;
  strokeMistakes?: number;
}

export interface ReviewResponse {
  userCardId: number;
  state: CardState;
  stability: number;
  difficulty: number;
  nextDue: string;
  scheduledDays: number;
  elapsedDays: number;
  newlyUnlocked?: AchievementView[];
}
