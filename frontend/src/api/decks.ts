import { api } from "@/api/client";

export interface DeckView {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  isSystem: boolean;
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

export async function fetchDecks(): Promise<DeckListResponse> {
  const response = await api.get<DeckListResponse>("/decks");
  return response.data;
}

export async function subscribeDeck(deckId: number): Promise<SubscribeResponse> {
  const response = await api.post<SubscribeResponse>(`/decks/${deckId}/subscribe`);
  return response.data;
}
