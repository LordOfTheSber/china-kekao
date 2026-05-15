import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { searchHanzi, type HanziSummary } from "@/api/hanzi";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Seal } from "@/components/ui/seal";
import { BrushDivider } from "@/components/ui/brush-divider";
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
    <div className="flex flex-col gap-4 max-w-3xl mx-auto w-full">
      <div>
        <h1 className="font-hanzi text-3xl sm:text-4xl font-bold tracking-tight text-ink">
          字典 <span className="text-2xl sm:text-3xl">Dictionary</span>
        </h1>
        <p className="text-sm text-ink-soft mt-1">
          Find published characters by hanzi, pinyin or English meaning.
        </p>
      </div>

      <div className="flex flex-col gap-3">
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
        <div className="flex flex-wrap gap-1.5">
          {HSK_OPTIONS.map((opt) => (
            <Button
              key={opt.label}
              size="sm"
              type="button"
              variant={hsk === opt.value ? "default" : "outline"}
              className={cn(
                "rounded-full",
                hsk === opt.value && "shadow-seal",
              )}
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
          <ul className={cn("flex flex-col", isFetching && "opacity-70 transition-opacity")}>
            {data.items.map((item, idx) => (
              <li key={item.id}>
                <SearchResultRow item={item} />
                {idx < data.items.length - 1 ? (
                  <BrushDivider className="my-1 px-6 opacity-60" />
                ) : null}
              </li>
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
    <Link
      to={`/hanzi/${item.id}`}
      className="group flex items-stretch gap-4 px-2 sm:px-4 py-4 rounded-brush transition-colors hover:bg-accent/30 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div
        className="font-hanzi font-bold text-[3.5rem] sm:text-7xl leading-none w-20 sm:w-28 shrink-0 text-center text-ink"
        lang="zh-Hans"
      >
        {item.character}
      </div>
      <div className="flex flex-col flex-1 min-w-0 justify-center gap-1">
        <div className="flex items-baseline gap-2 flex-wrap">
          <span className="font-hanzi text-lg font-medium text-seal truncate">
            {item.pinyin}
          </span>
          {item.hskLevel != null ? (
            <Seal size="sm" tilt={false} title={`HSK ${item.hskLevel}`}>
              {item.hskLevel}
            </Seal>
          ) : null}
          {item.strokeCount != null ? (
            <span className="text-xs text-ink-soft">
              {item.strokeCount} strokes
            </span>
          ) : null}
        </div>
        <div className="text-sm text-ink-soft leading-snug">
          {item.meaningsEn.length ? item.meaningsEn.join(", ") : "—"}
        </div>
      </div>
    </Link>
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
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
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
