import { api } from "@/api/client";

export interface HanziSummary {
  id: number;
  character: string;
  pinyin: string;
  strokeCount: number | null;
  hskLevel: number | null;
  frequencyRank: number | null;
  meaningsEn: string[];
  hasStrokeData: boolean;
}

export interface HanziSearchPage {
  items: HanziSummary[];
  page: number;
  size: number;
  total: number;
}

export interface HanziExampleView {
  sentence: string;
  pinyin: string;
  translation: string;
}

export interface UserCardSummary {
  userCardId: number;
  mode: "RECOGNITION" | "PRODUCTION";
  state: "NEW" | "LEARNING" | "REVIEW" | "RELEARNING";
  dueDate: string | null;
}

export interface HanziDetail {
  id: number;
  character: string;
  pinyin: string;
  strokeCount: number | null;
  hskLevel: number | null;
  frequencyRank: number | null;
  meaningsEn: string[];
  hasStrokeData: boolean;
  examples: HanziExampleView[];
  userCards: UserCardSummary[];
}

export async function searchHanzi(params: {
  q: string;
  hsk?: number | null;
  page?: number;
  size?: number;
}): Promise<HanziSearchPage> {
  const response = await api.get<HanziSearchPage>("/hanzi/search", {
    params: {
      q: params.q,
      hsk: params.hsk ?? undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
  return response.data;
}

export async function fetchHanziDetail(id: number | string): Promise<HanziDetail> {
  const response = await api.get<HanziDetail>(`/hanzi/${id}`);
  return response.data;
}
