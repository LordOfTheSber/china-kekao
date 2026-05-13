import { api } from "@/api/client";
import type { ReviewRequest, ReviewResponse, StudyCard } from "@/api/types";

export async function fetchStudySession(deckId?: number): Promise<StudyCard[]> {
  const response = await api.get<StudyCard[]>("/study/session", {
    params: deckId != null ? { deckId } : undefined,
  });
  return response.data;
}

export async function postReview(req: ReviewRequest): Promise<ReviewResponse> {
  const response = await api.post<ReviewResponse>("/study/review", req);
  return response.data;
}

export interface DistractorsResponse {
  hanziId: number;
  distractors: string[];
}

export async function fetchDistractors(hanziId: number, count = 5): Promise<DistractorsResponse> {
  const response = await api.get<DistractorsResponse>("/study/distractors", {
    params: { hanziId, count },
  });
  return response.data;
}
