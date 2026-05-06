import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { searchHanzi, type HanziSummary } from "@/api/hanzi";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

const HSK_OPTIONS: Array<{ value: number | null; label: string }> = [
  { value: null, label: "All HSK" },
  { value: 1, label: "HSK 1" },
  { value: 2, label: "HSK 2" },
  { value: 3, label: "HSK 3" },
  { value: 4, label: "HSK 4" },
  { value: 5, label: "HSK 5" },
  { value: 6, label: "HSK 6" },
];

const PAGE_SIZE = 20;

export function SearchPage() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [hsk, setHsk] = useState<number | null>(null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    const handle = window.setTimeout(() => {
      setDebouncedQuery(query.trim());
      setPage(0);
    }, 250);
    return () => window.clearTimeout(handle);
  }, [query]);

  useEffect(() => {
    setPage(0);
  }, [hsk]);

  const { data, isFetching, isError, refetch } = useQuery({
    queryKey: ["hanzi-search", debouncedQuery, hsk, page],
    queryFn: () =>
      searchHanzi({ q: debouncedQuery, hsk: hsk ?? undefined, page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <div className="flex flex-col gap-4 max-w-3xl mx-auto">
      <div>
        <h1 className="text-2xl font-semibold">Search</h1>
        <p className="text-sm text-muted-foreground">
          Find published characters by hanzi, pinyin or English meaning.
        </p>
      </div>

      <div className="flex flex-col sm:flex-row gap-2">
        <Input
          autoFocus
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="e.g. ni, 你, hello"
          autoComplete="off"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
        />
        <div className="flex flex-wrap gap-1">
          {HSK_OPTIONS.map((opt) => (
            <Button
              key={opt.label}
              size="sm"
              type="button"
              variant={hsk === opt.value ? "default" : "outline"}
              onClick={() => setHsk(opt.value)}
            >
              {opt.label}
            </Button>
          ))}
        </div>
      </div>

      {isError ? (
        <Card>
          <CardContent className="pt-6 flex items-center justify-between">
            <p className="text-sm text-destructive">Search failed.</p>
            <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
          </CardContent>
        </Card>
      ) : !data ? (
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground">Type to search…</p>
          </CardContent>
        </Card>
      ) : data.items.length === 0 ? (
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground">
              No published hanzi match {debouncedQuery ? `“${debouncedQuery}”` : "your filters"}.
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <ul className={cn("flex flex-col gap-2", isFetching && "opacity-70 transition-opacity")}>
            {data.items.map((item) => (
              <SearchResultRow key={item.id} item={item} />
            ))}
          </ul>
          <PaginationBar
            page={data.page}
            totalPages={totalPages}
            total={data.total}
            onChange={setPage}
            disabled={isFetching}
          />
        </>
      )}
    </div>
  );
}

function SearchResultRow({ item }: { item: HanziSummary }) {
  return (
    <li>
      <Link
        to={`/hanzi/${item.id}`}
        className="block rounded-md border p-3 hover:bg-accent focus:outline-none focus:ring-2 focus:ring-ring"
      >
        <div className="flex items-center gap-4">
          <div className="text-4xl font-serif w-14 text-center" lang="zh-Hans">
            {item.character}
          </div>
          <div className="flex flex-col flex-1 min-w-0">
            <div className="flex items-baseline gap-2 flex-wrap">
              <span className="font-medium">{item.pinyin}</span>
              {item.hskLevel != null ? (
                <span className="text-xs uppercase tracking-wide text-muted-foreground">
                  HSK {item.hskLevel}
                </span>
              ) : null}
              {item.strokeCount != null ? (
                <span className="text-xs text-muted-foreground">
                  {item.strokeCount} strokes
                </span>
              ) : null}
            </div>
            <div className="text-sm text-muted-foreground truncate">
              {item.meaningsEn.length ? item.meaningsEn.join(", ") : "—"}
            </div>
          </div>
        </div>
      </Link>
    </li>
  );
}

function PaginationBar({
  page,
  totalPages,
  total,
  onChange,
  disabled,
}: {
  page: number;
  totalPages: number;
  total: number;
  onChange: (next: number) => void;
  disabled: boolean;
}) {
  const range = useMemo(() => `${page * PAGE_SIZE + 1}–${Math.min((page + 1) * PAGE_SIZE, total)} of ${total}`, [page, total]);
  return (
    <div className="flex items-center justify-between text-sm text-muted-foreground">
      <span>{range}</span>
      <div className="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={disabled || page === 0}
          onClick={() => onChange(page - 1)}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={disabled || page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
