import { api } from "@/api/client";

export interface DeckView {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  isSystem: boolean;
  owned: boolean;
  hanziCount: number;
  subscribed: boolean;
}

export interface DeckListResponse {
  decks: DeckView[];
}

export interface SubscribeResponse {
  deckId: number;
  newlyCreatedCards: number;
  totalCardsInDeck: number;
  alreadySubscribed: boolean;
}

export interface DeckHanziView {
  hanziId: number;
  character: string;
  pinyin: string;
  hskLevel: number | null;
  position: number;
  meaningsEn: string[];
}

export interface DeckDetailView {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  isSystem: boolean;
  owned: boolean;
  subscribed: boolean;
  hanziCount: number;
  entries: DeckHanziView[];
}

export interface CreateDeckRequest {
  name: string;
  description?: string | null;
}

export type UpdateDeckRequest = CreateDeckRequest;

export async function fetchDecks(): Promise<DeckListResponse> {
  const response = await api.get<DeckListResponse>("/decks");
  return response.data;
}

export async function fetchDeck(deckId: number): Promise<DeckDetailView> {
  const response = await api.get<DeckDetailView>(`/decks/${deckId}`);
  return response.data;
}

export async function subscribeDeck(deckId: number): Promise<SubscribeResponse> {
  const response = await api.post<SubscribeResponse>(`/decks/${deckId}/subscribe`);
  return response.data;
}

export async function createDeck(payload: CreateDeckRequest): Promise<DeckView> {
  const response = await api.post<DeckView>("/decks", payload);
  return response.data;
}

export async function updateDeck(
  deckId: number,
  payload: UpdateDeckRequest,
): Promise<DeckView> {
  const response = await api.patch<DeckView>(`/decks/${deckId}`, payload);
  return response.data;
}

export async function deleteDeck(deckId: number): Promise<void> {
  await api.delete(`/decks/${deckId}`);
}

export async function addDeckHanzi(
  deckId: number,
  hanziIds: number[],
): Promise<DeckDetailView> {
  const response = await api.post<DeckDetailView>(`/decks/${deckId}/hanzi`, {
    hanziIds,
  });
  return response.data;
}

export async function removeDeckHanzi(
  deckId: number,
  hanziId: number,
): Promise<void> {
  await api.delete(`/decks/${deckId}/hanzi/${hanziId}`);
}
