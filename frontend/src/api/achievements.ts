import { api } from "@/api/client";

export type AchievementCategory =
  | "LEARNED"
  | "STREAK"
  | "REVIEWS"
  | "SPECIAL";

export interface AchievementView {
  code: string;
  name: string;
  description: string;
  glyph: string;
  category: AchievementCategory;
  threshold: number | null;
  sortOrder: number;
  unlocked: boolean;
  unlockedAt: string | null;
}

export interface AchievementListResponse {
  items: AchievementView[];
  unlocked: number;
  total: number;
}

export interface ClaimRequest {
  localHour?: number;
  againCount?: number;
  hardCount?: number;
  totalCount?: number;
}

export interface ClaimResponse {
  unlocked: boolean;
  code: string;
}

export async function fetchAchievements(): Promise<AchievementListResponse> {
  const response = await api.get<AchievementListResponse>("/achievements");
  return response.data;
}

export async function claimAchievement(
  code: string,
  req?: ClaimRequest,
): Promise<ClaimResponse> {
  const response = await api.post<ClaimResponse>(
    `/achievements/claim/${encodeURIComponent(code)}`,
    req ?? {},
  );
  return response.data;
}
