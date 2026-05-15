import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  addDeckHanzi,
  deleteDeck,
  fetchDeck,
  removeDeckHanzi,
  subscribeDeck,
  unsubscribeDeck,
  updateDeck,
  type DeckDetailView,
} from "@/api/decks";
import { searchHanzi, type HanziSummary } from "@/api/hanzi";
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

export function DeckEditPage() {
  const { id } = useParams();
  const deckId = id ? Number(id) : NaN;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [addOpen, setAddOpen] = useState(false);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["deck", deckId],
    queryFn: () => fetchDeck(deckId),
    enabled: Number.isFinite(deckId),
  });

  if (!Number.isFinite(deckId)) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-destructive">
          Invalid deck id.
        </CardContent>
      </Card>
    );
  }

  if (isLoading) {
    return (
      <Card>
        <CardContent className="pt-6 h-32 animate-pulse bg-muted/40" />
      </Card>
    );
  }
  if (isError || !data) {
    return (
      <Card>
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load deck.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-4 max-w-3xl mx-auto w-full">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Button asChild variant="ghost" size="sm">
          <Link to="/decks">← All decks</Link>
        </Button>
        <div className="flex flex-wrap items-center gap-2 justify-end">
          <SubscribeToggleButton deck={data} />
          {data.owned ? (
            <DeleteDeckButton
              deckId={data.id}
              deckName={data.name}
              onDeleted={() => {
                queryClient.invalidateQueries({ queryKey: ["decks"] });
                navigate("/decks");
              }}
            />
          ) : null}
        </div>
      </div>

      <DeckMetaCard deck={data} editable={data.owned} />

      <Card>
        <CardHeader>
          <CardTitle className="flex flex-wrap items-center justify-between gap-2">
            <span className="min-w-0 break-words">Hanzi ({data.entries.length})</span>
            {data.owned ? (
              <Button size="sm" onClick={() => setAddOpen(true)}>
                + Add hanzi
              </Button>
            ) : null}
          </CardTitle>
          <CardDescription>
            {data.owned
              ? "Add or remove characters. Subscribed users automatically get the new cards."
              : "This is a system deck and its content cannot be edited here."}
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          {data.entries.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No hanzi yet. Use <em>Add hanzi</em> to search for characters and
              include them in this deck.
            </p>
          ) : (
            <ul className="flex flex-col divide-y">
              {data.entries.map((entry) => (
                <li
                  key={entry.hanziId}
                  className="flex items-center gap-3 py-2"
                >
                  <Link
                    to={`/hanzi/${entry.hanziId}`}
                    className="shrink-0 text-3xl font-semibold hover:underline"
                    lang="zh-Hans"
                  >
                    {entry.character}
                  </Link>
                  <div className="flex flex-1 flex-col min-w-0">
                    <span className="text-sm text-muted-foreground truncate">
                      {entry.pinyin}
                      {entry.hskLevel != null
                        ? ` · HSK ${entry.hskLevel}`
                        : ""}
                    </span>
                    <span className="text-xs text-muted-foreground truncate">
                      {entry.meaningsEn.slice(0, 3).join(", ")}
                    </span>
                  </div>
                  {data.owned ? (
                    <RemoveHanziButton
                      deckId={data.id}
                      hanziId={entry.hanziId}
                    />
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      {data.owned ? (
        <AddHanziDialog
          deckId={data.id}
          existingIds={data.entries.map((e) => e.hanziId)}
          open={addOpen}
          onOpenChange={setAddOpen}
        />
      ) : null}
    </div>
  );
}

function DeckMetaCard({
  deck,
  editable,
}: {
  deck: DeckDetailView;
  editable: boolean;
}) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(deck.name);
  const [description, setDescription] = useState(deck.description ?? "");

  const mutation = useMutation({
    mutationFn: () =>
      updateDeck(deck.id, {
        name: name.trim(),
        description: description.trim() || null,
      }),
    onSuccess: () => {
      toast({ title: "Deck updated" });
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      queryClient.invalidateQueries({ queryKey: ["deck", deck.id] });
      setEditing(false);
    },
    onError: (error) => {
      toast({
        title: "Could not update deck",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  if (!editing) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex flex-wrap items-center justify-between gap-2">
            <span className="min-w-0 break-words">{deck.name}</span>
            {editable ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setName(deck.name);
                  setDescription(deck.description ?? "");
                  setEditing(true);
                }}
              >
                Rename
              </Button>
            ) : null}
          </CardTitle>
          <CardDescription>
            {deck.description ?? "No description."}
          </CardDescription>
        </CardHeader>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="pt-6">
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
              value={name}
              maxLength={128}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">Description</span>
            <textarea
              value={description}
              maxLength={4000}
              onChange={(e) => setDescription(e.target.value)}
              className="min-h-[72px] w-full resize-y rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors hover:border-ring/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </label>
          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setEditing(false)}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={!name.trim() || mutation.isPending}>
              {mutation.isPending ? "Saving…" : "Save"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

function RemoveHanziButton({
  deckId,
  hanziId,
}: {
  deckId: number;
  hanziId: number;
}) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => removeDeckHanzi(deckId, hanziId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["deck", deckId] });
      queryClient.invalidateQueries({ queryKey: ["decks"] });
    },
    onError: (error) => {
      toast({
        title: "Remove failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });
  return (
    <Button
      size="sm"
      variant="ghost"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Remove
    </Button>
  );
}

function SubscribeToggleButton({ deck }: { deck: DeckDetailView }) {
  const queryClient = useQueryClient();

  // The study-session query already refetches on mount (staleTime: 0), so invalidating it
  // here would only trigger an extra round-trip while the user is still on this page.
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["decks"] });
    queryClient.invalidateQueries({ queryKey: ["deck", deck.id] });
    queryClient.invalidateQueries({ queryKey: ["dashboard"] });
  };

  const subscribe = useMutation({
    mutationFn: () => subscribeDeck(deck.id),
    onSuccess: (resp) => {
      toast({
        title: resp.alreadySubscribed ? "Already subscribed" : "Subscribed",
        description:
          resp.newlyCreatedCards > 0
            ? `Added ${resp.newlyCreatedCards} new cards to your queue.`
            : "Your queue already contains every card from this deck.",
      });
      invalidate();
    },
    onError: (error) => {
      toast({
        title: "Subscribe failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  const unsubscribe = useMutation({
    mutationFn: () => unsubscribeDeck(deck.id),
    onSuccess: () => {
      toast({
        title: "Unsubscribed",
        description: "Your existing study progress is preserved.",
      });
      invalidate();
    },
    onError: (error) => {
      toast({
        title: "Unsubscribe failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  const busy = subscribe.isPending || unsubscribe.isPending;

  if (deck.subscribed) {
    return (
      <Button
        variant="outline"
        size="sm"
        disabled={busy}
        onClick={() => {
          if (
            confirm(
              `Unsubscribe from “${deck.name}”? Your existing study progress is preserved.`,
            )
          ) {
            unsubscribe.mutate();
          }
        }}
      >
        Unsubscribe
      </Button>
    );
  }
  return (
    <Button
      size="sm"
      disabled={busy || deck.entries.length === 0}
      onClick={() => subscribe.mutate()}
      title={
        deck.entries.length === 0
          ? "Add hanzi to this deck before subscribing"
          : undefined
      }
    >
      Subscribe
    </Button>
  );
}

function DeleteDeckButton({
  deckId,
  deckName,
  onDeleted,
}: {
  deckId: number;
  deckName: string;
  onDeleted: () => void;
}) {
  const mutation = useMutation({
    mutationFn: () => deleteDeck(deckId),
    onSuccess: () => {
      toast({ title: "Deck deleted", description: `“${deckName}” is gone.` });
      onDeleted();
    },
    onError: (error) => {
      toast({
        title: "Delete failed",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });
  return (
    <Button
      variant="destructive"
      size="sm"
      disabled={mutation.isPending}
      onClick={() => {
        if (confirm(`Delete deck “${deckName}”? This cannot be undone.`)) {
          mutation.mutate();
        }
      }}
    >
      Delete deck
    </Button>
  );
}

function AddHanziDialog({
  deckId,
  existingIds,
  open,
  onOpenChange,
}: {
  deckId: number;
  existingIds: number[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<HanziSummary[]>([]);

  const existingSet = useMemo(() => new Set(existingIds), [existingIds]);
  const selectedSet = useMemo(
    () => new Set(selected.map((h) => h.id)),
    [selected],
  );

  const trimmed = query.trim();
  const search = useQuery({
    queryKey: ["hanzi-search-deck-add", trimmed],
    queryFn: () => searchHanzi({ q: trimmed, page: 0, size: 20 }),
    enabled: open && trimmed.length > 0,
  });

  const mutation = useMutation({
    mutationFn: () =>
      addDeckHanzi(
        deckId,
        selected.map((h) => h.id),
      ),
    onSuccess: (deck) => {
      toast({
        title: "Hanzi added",
        description: `Deck now contains ${deck.hanziCount} characters.`,
      });
      // The mutation response already contains the fresh deck detail, so prime the cache
      // instead of triggering a follow-up GET /api/decks/{id}.
      queryClient.setQueryData(["deck", deckId], deck);
      queryClient.invalidateQueries({ queryKey: ["decks"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      setSelected([]);
      setQuery("");
      onOpenChange(false);
    },
    onError: (error) => {
      toast({
        title: "Could not add hanzi",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          setSelected([]);
          setQuery("");
        }
        onOpenChange(next);
      }}
    >
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Add hanzi to deck</DialogTitle>
          <DialogDescription>
            Search by character, pinyin, or English meaning. Pick the
            characters you want to add and confirm.
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-3">
          <Input
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="e.g. 你, ni, hello"
          />

          {selected.length > 0 ? (
            <div className="flex flex-wrap gap-1">
              {selected.map((h) => (
                <button
                  key={h.id}
                  type="button"
                  className="rounded-full border px-2 py-0.5 text-sm hover:bg-accent"
                  onClick={() =>
                    setSelected((prev) => prev.filter((x) => x.id !== h.id))
                  }
                  title="Click to remove"
                >
                  {h.character} ✕
                </button>
              ))}
            </div>
          ) : null}

          <div className="max-h-72 overflow-y-auto rounded-md border">
            {trimmed.length === 0 ? (
              <p className="p-3 text-sm text-muted-foreground">
                Type to search published hanzi.
              </p>
            ) : search.isLoading ? (
              <p className="p-3 text-sm text-muted-foreground">Searching…</p>
            ) : search.data && search.data.items.length > 0 ? (
              <ul className="divide-y">
                {search.data.items.map((h) => {
                  const already = existingSet.has(h.id);
                  const picked = selectedSet.has(h.id);
                  return (
                    <li
                      key={h.id}
                      className="flex items-center gap-3 px-3 py-2"
                    >
                      <span className="text-2xl shrink-0" lang="zh-Hans">{h.character}</span>
                      <div className="flex flex-1 flex-col min-w-0">
                        <span className="text-sm truncate">
                          {h.pinyin}
                          {h.hskLevel != null ? ` · HSK ${h.hskLevel}` : ""}
                        </span>
                        <span className="text-xs text-muted-foreground truncate">
                          {h.meaningsEn.slice(0, 3).join(", ")}
                        </span>
                      </div>
                      {already ? (
                        <span className="text-xs text-muted-foreground">
                          In deck
                        </span>
                      ) : picked ? (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() =>
                            setSelected((prev) =>
                              prev.filter((x) => x.id !== h.id),
                            )
                          }
                        >
                          Remove
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          onClick={() => setSelected((prev) => [...prev, h])}
                        >
                          Add
                        </Button>
                      )}
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className="p-3 text-sm text-muted-foreground">No matches.</p>
            )}
          </div>

          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>
            <Button
              disabled={selected.length === 0 || mutation.isPending}
              onClick={() => mutation.mutate()}
            >
              {mutation.isPending
                ? "Adding…"
                : `Add ${selected.length || ""} hanzi`}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
