import { create } from "zustand";
import { useEffect } from "react";

import { cn } from "@/lib/utils";

type ToastVariant = "default" | "destructive" | "success";

interface Toast {
  id: number;
  title: string;
  description?: string;
  variant: ToastVariant;
}

interface ToastState {
  toasts: Toast[];
  push: (toast: Omit<Toast, "id">) => void;
  dismiss: (id: number) => void;
}

const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  push: (toast) =>
    set((state) => ({
      toasts: [...state.toasts, { ...toast, id: Date.now() + Math.random() }],
    })),
  dismiss: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}));

export function toast(input: { title: string; description?: string; variant?: ToastVariant }) {
  useToastStore.getState().push({
    title: input.title,
    description: input.description,
    variant: input.variant ?? "default",
  });
}

function ToastItem({ toast: t }: { toast: Toast }) {
  const dismiss = useToastStore((s) => s.dismiss);
  useEffect(() => {
    const timer = setTimeout(() => dismiss(t.id), 4000);
    return () => clearTimeout(timer);
  }, [dismiss, t.id]);

  return (
    <div
      role="status"
      className={cn(
        "pointer-events-auto rounded-lg border bg-card p-4 shadow-card-hover w-full sm:min-w-[280px] sm:w-auto sm:max-w-sm animate-in fade-in-0 slide-in-from-top-2",
        t.variant === "destructive" && "border-destructive/60 bg-destructive text-destructive-foreground",
        t.variant === "success" && "border-emerald-500/60",
      )}
      onClick={() => dismiss(t.id)}
    >
      <div className="text-sm font-medium">{t.title}</div>
      {t.description ? (
        <div className="text-xs opacity-90 mt-1">{t.description}</div>
      ) : null}
    </div>
  );
}

export function Toaster() {
  const toasts = useToastStore((s) => s.toasts);
  return (
    <div className="pointer-events-none fixed inset-x-3 top-3 sm:inset-auto sm:top-4 sm:right-4 z-[100] flex flex-col gap-2 items-stretch sm:items-end">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} />
      ))}
    </div>
  );
}
