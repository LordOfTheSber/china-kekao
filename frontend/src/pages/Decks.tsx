import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { fetchDecks, subscribeDeck, type DeckView } from "@/api/decks";
import { extractErrorMessage } from "@/api/auth";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { toast } from "@/components/Toaster";

export function DecksPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["decks"],
    queryFn: fetchDecks,
  });

  const mutation = useMutation({
    mutationFn: subscribeDeck,
    onSuccess: (resp, deckId) => {
      const wasNew = !resp.alreadySubscribed;
      toast({
        title: wasNew ? "Subscribed" : "Already subscribed",
        description: resp.newlyCreatedCards > 0
          ? `Added ${resp.newlyCreatedCards} new cards to your queue.`
          : "Your queue already contains every card from this deck.",
      });
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["study-session"] });
      void deckId;
    },
    onError: (error) => {
      toast({
        title: "Subscribe failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  if (isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="pt-6 h-24 animate-pulse bg-muted/40" />
          </Card>
        ))}
      </div>
    );
  }
  if (isError || !data) {
    return (
      <Card>
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load decks.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </CardContent>
      </Card>
    );
  }
  if (data.decks.length === 0) {
    return (
      <Card>
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground">No system decks available yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-4 max-w-3xl mx-auto">
      <div>
        <h1 className="text-2xl font-semibold">Decks</h1>
        <p className="text-sm text-muted-foreground">
          Subscribe to add every card in a deck to your study queue (Recognition + Production).
        </p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {data.decks.map((deck) => (
          <DeckCard
            key={deck.id}
            deck={deck}
            disabled={mutation.isPending}
            onSubscribe={() => mutation.mutate(deck.id)}
          />
        ))}
      </div>
    </div>
  );
}

function DeckCard({
  deck,
  disabled,
  onSubscribe,
}: {
  deck: DeckView;
  disabled: boolean;
  onSubscribe: () => void;
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg flex items-center justify-between gap-2">
          <span>{deck.name}</span>
          {deck.subscribed ? (
            <span className="text-xs uppercase tracking-wide text-emerald-600">
              Subscribed
            </span>
          ) : null}
        </CardTitle>
        <CardDescription>
          {deck.description ?? `${deck.hanziCount} hanzi`}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex items-center justify-between">
        <span className="text-xs text-muted-foreground">
          {deck.hanziCount} hanzi
        </span>
        <Button
          size="sm"
          variant={deck.subscribed ? "outline" : "default"}
          disabled={disabled}
          onClick={onSubscribe}
        >
          {deck.subscribed ? "Re-sync" : "Subscribe"}
        </Button>
      </CardContent>
    </Card>
  );
}
