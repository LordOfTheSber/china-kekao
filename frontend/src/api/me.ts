import { api } from "@/api/client";

export interface UserSettings {
  newPerDay: number;
  maxReviewsPerDay: number;
  requestRetention: number;
  productionMode: "DRAWING" | "CHOICE";
  drawingHelpLevel: "STRICT" | "NORMAL" | "EASY";
  withTones: boolean;
}

export type UserSettingsPatch = Partial<UserSettings>;

export async function fetchUserSettings(): Promise<UserSettings> {
  const response = await api.get<UserSettings>("/me/settings");
  return response.data;
}

export async function updateUserSettings(
  patch: UserSettingsPatch,
): Promise<UserSettings> {
  const response = await api.patch<UserSettings>("/me/settings", patch);
  return response.data;
}
