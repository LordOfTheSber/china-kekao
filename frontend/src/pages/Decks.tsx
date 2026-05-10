import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  createDeck,
  fetchDecks,
  subscribeDeck,
  unsubscribeDeck,
  type DeckView,
} from "@/api/decks";
import { extractErrorMessage } from "@/api/auth";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { toast } from "@/components/Toaster";

export function DecksPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["decks"],
    queryFn: fetchDecks,
  });

  const subscribeMutation = useMutation({
    mutationFn: subscribeDeck,
    onSuccess: (resp) => {
      const wasNew = !resp.alreadySubscribed;
      toast({
        title: wasNew ? "Subscribed" : "Already subscribed",
        description:
          resp.newlyCreatedCards > 0
            ? `Added ${resp.newlyCreatedCards} new cards to your queue.`
            : "Your queue already contains every card from this deck.",
      });
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["study-session"] });
    },
    onError: (error) => {
      toast({
        title: "Subscribe failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  const unsubscribeMutation = useMutation({
    mutationFn: unsubscribeDeck,
    onSuccess: () => {
      toast({
        title: "Unsubscribed",
        description:
          "Existing cards stay in your queue, but new hanzi from this deck won't be added automatically.",
      });
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["study-session"] });
    },
    onError: (error) => {
      toast({
        title: "Unsubscribe failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  const busy = subscribeMutation.isPending || unsubscribeMutation.isPending;

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
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  const systemDecks = data.decks.filter((d) => d.isSystem);
  const myDecks = data.decks.filter((d) => d.owned);

  return (
    <div className="flex flex-col gap-6 max-w-3xl mx-auto">
      <div className="flex items-end justify-between gap-2">
        <div>
          <h1 className="text-2xl font-semibold">Decks</h1>
          <p className="text-sm text-muted-foreground">
            Subscribe to add every card in a deck to your study queue
            (Recognition + Production), or build your own.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>New deck</Button>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-medium">My decks</h2>
        {myDecks.length === 0 ? (
          <Card>
            <CardContent className="pt-6 text-sm text-muted-foreground">
              You haven&apos;t created any decks yet. Click <em>New deck</em> to
              start a custom collection.
            </CardContent>
          </Card>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {myDecks.map((deck) => (
              <DeckCard
                key={deck.id}
                deck={deck}
                disabled={busy}
                onSubscribe={() => subscribeMutation.mutate(deck.id)}
                onUnsubscribe={() => unsubscribeMutation.mutate(deck.id)}
              />
            ))}
          </div>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-medium">System decks</h2>
        {systemDecks.length === 0 ? (
          <Card>
            <CardContent className="pt-6 text-sm text-muted-foreground">
              No system decks available yet.
            </CardContent>
          </Card>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {systemDecks.map((deck) => (
              <DeckCard
                key={deck.id}
                deck={deck}
                disabled={busy}
                onSubscribe={() => subscribeMutation.mutate(deck.id)}
                onUnsubscribe={() => unsubscribeMutation.mutate(deck.id)}
              />
            ))}
          </div>
        )}
      </section>

      <CreateDeckDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}

function DeckCard({
  deck,
  disabled,
  onSubscribe,
  onUnsubscribe,
}: {
  deck: DeckView;
  disabled: boolean;
  onSubscribe: () => void;
  onUnsubscribe: () => void;
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
      <CardContent className="flex items-center justify-between gap-2">
        <span className="text-xs text-muted-foreground">
          {deck.hanziCount} hanzi
        </span>
        <div className="flex items-center gap-2">
          {deck.owned ? (
            <Button asChild size="sm" variant="outline">
              <Link to={`/decks/${deck.id}`}>Edit</Link>
            </Button>
          ) : null}
          {deck.subscribed ? (
            <Button
              size="sm"
              variant="ghost"
              disabled={disabled}
              onClick={() => {
                if (
                  confirm(
                    `Unsubscribe from “${deck.name}”? Your existing study progress is preserved.`,
                  )
                ) {
                  onUnsubscribe();
                }
              }}
            >
              Unsubscribe
            </Button>
          ) : null}
          <Button
            size="sm"
            variant={deck.subscribed ? "outline" : "default"}
            disabled={disabled || deck.hanziCount === 0}
            onClick={onSubscribe}
            title={
              deck.hanziCount === 0
                ? "Add hanzi to this deck before subscribing"
                : undefined
            }
          >
            {deck.subscribed ? "Re-sync" : "Subscribe"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function CreateDeckDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      createDeck({
        name: name.trim(),
        description: description.trim() || null,
      }),
    onSuccess: (deck) => {
      toast({
        title: "Deck created",
        description: `“${deck.name}” is ready — add some hanzi to it.`,
      });
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      setName("");
      setDescription("");
      onOpenChange(false);
    },
    onError: (error) => {
      toast({
        title: "Could not create deck",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Create a custom deck</DialogTitle>
          <DialogDescription>
            Give your deck a name and an optional description. You can add hanzi
            on the next screen.
          </DialogDescription>
        </DialogHeader>
        <form
          className="flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            if (!name.trim()) return;
            mutation.mutate();
          }}
        >
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">Name</span>
            <Input
              autoFocus
              value={name}
              maxLength={128}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Travel basics"
              required
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">
              Description{" "}
              <span className="text-muted-foreground font-normal">
                (optional)
              </span>
            </span>
            <textarea
              value={description}
              maxLength={4000}
              onChange={(e) => setDescription(e.target.value)}
              className="min-h-[72px] rounded-md border border-input bg-background px-3 py-2 text-sm"
              placeholder="What is this deck for?"
            />
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={!name.trim() || mutation.isPending}>
              {mutation.isPending ? "Creating…" : "Create deck"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
