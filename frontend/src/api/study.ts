import { api } from "@/api/client";
import type { ReviewRequest, ReviewResponse, StudyCard } from "@/api/types";

export async function fetchStudySession(): Promise<StudyCard[]> {
  const response = await api.get<StudyCard[]>("/study/session");
  return response.data;
}

export async function postReview(req: ReviewRequest): Promise<ReviewResponse> {
  const response = await api.post<ReviewResponse>("/study/review", req);
  return response.data;
}
