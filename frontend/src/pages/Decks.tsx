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
import { Seal } from "@/components/ui/seal";
import { HanziLoader } from "@/components/HanziLoader";
import { toast } from "@/components/Toaster";

function pickWatermark(name: string): string {
  for (const ch of name) {
    const code = ch.codePointAt(0);
    if (code && code >= 0x4e00 && code <= 0x9fff) return ch;
  }
  return "永";
}

function hskLevelOf(name: string): number | null {
  const m = name.match(/HSK\s*(\d)/i);
  return m ? Number(m[1]) : null;
}

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
      <div className="min-h-[40vh] flex items-center justify-center">
        <HanziLoader size={96} label="Gathering your decks…" />
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
    <div className="flex flex-col gap-6 max-w-3xl mx-auto w-full">
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3">
        <div className="min-w-0">
          <h1 className="font-hanzi text-3xl sm:text-4xl font-bold tracking-tight text-ink">
            册 <span className="text-2xl sm:text-3xl">Decks</span>
          </h1>
          <p className="text-sm text-ink-soft mt-1">
            Subscribe to add every card in a deck to your study queue
            (Recognition + Production), or build your own.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)} className="w-full sm:w-auto shrink-0">
          + New deck
        </Button>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="font-hanzi text-xl font-bold text-ink">My decks</h2>
        {myDecks.length === 0 ? (
          <Card>
            <CardContent className="pt-6 flex flex-col items-center gap-3 text-center">
              <div className="font-hanzi text-6xl text-ink/30 leading-none">始</div>
              <p className="text-sm text-ink-soft">
                You haven&apos;t created any decks yet. Click <em>New deck</em> above
                to start a custom collection.
              </p>
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
        <h2 className="font-hanzi text-xl font-bold text-ink">System decks</h2>
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
  const hsk = deck.isSystem ? hskLevelOf(deck.name) : null;
  const watermark = pickWatermark(deck.name);
  return (
    <Card className="group relative flex flex-col overflow-hidden bg-paper-elevated shadow-card transition-all duration-300 hover:-translate-y-1 hover:rotate-[-0.4deg] hover:shadow-tactile motion-reduce:hover:transform-none">
      <span
        aria-hidden
        className="pointer-events-none absolute -right-2 -bottom-6 font-hanzi text-[9rem] leading-none text-watermark opacity-[0.04] transition-opacity duration-300 group-hover:opacity-[0.12]"
      >
        {watermark}
      </span>
      {hsk !== null ? (
        <Seal
          size="sm"
          className="absolute top-3 right-3 z-10"
          title={`HSK ${hsk}`}
        >
          {hsk}
        </Seal>
      ) : null}
      <CardHeader className="pb-3 relative">
        <CardTitle className="font-hanzi text-base sm:text-lg flex items-start justify-between gap-2 pr-12">
          <span className="min-w-0 break-words">{deck.name}</span>
        </CardTitle>
        <CardDescription className="line-clamp-2">
          {deck.description ?? `${deck.hanziCount} hanzi`}
        </CardDescription>
        {deck.subscribed ? (
          <span className="absolute right-3 top-12 text-[10px] font-bold uppercase tracking-widest text-success">
            订
          </span>
        ) : null}
      </CardHeader>
      <CardContent className="mt-auto flex flex-wrap items-center justify-between gap-2 relative">
        <span className="text-xs text-ink-soft tabular-nums">
          {deck.hanziCount} hanzi
        </span>
        <div className="flex flex-wrap items-center gap-2 justify-end">
          {deck.owned ? (
            <Button asChild size="sm" variant="outline">
              <Link to={`/decks/${deck.id}`}>Edit</Link>
            </Button>
          ) : null}
          {deck.subscribed && deck.hanziCount > 0 ? (
            <Button asChild size="sm" variant="secondary">
              <Link to={`/study?deckId=${deck.id}`}>Practice</Link>
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
              className="min-h-[72px] w-full resize-y rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors hover:border-ring/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
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
