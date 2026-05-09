import { useEffect } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/auth";
import { usePreferencesStore } from "@/store/preferences";
import { fetchUserSettings } from "@/api/me";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/study", label: "Study" },
  { to: "/decks", label: "Decks" },
  { to: "/search", label: "Search" },
  { to: "/stats", label: "Stats" },
  { to: "/settings", label: "Settings" },
];

export function Layout() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const hydrate = usePreferencesStore((s) => s.hydrate);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    fetchUserSettings()
      .then((settings) => {
        if (cancelled) return;
        hydrate({
          newPerDay: settings.newPerDay,
          maxReviewsPerDay: settings.maxReviewsPerDay,
          requestRetention: settings.requestRetention,
          productionMode: settings.productionMode,
          helpLevel: settings.drawingHelpLevel,
          withTones: settings.withTones,
        });
      })
      .catch(() => {
        // Non-fatal: keep persisted defaults.
      });
    return () => {
      cancelled = true;
    };
  }, [user, hydrate]);

  const handleLogout = () => {
    clear();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-background to-muted/40">
      <header className="sticky top-0 z-30 border-b bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-14 items-center justify-between gap-4">
          <NavLink
            to="/"
            className="flex items-center gap-2 font-semibold tracking-tight"
          >
            <span
              className="inline-flex h-7 w-7 items-center justify-center rounded-md bg-primary text-primary-foreground text-base font-serif"
              aria-hidden
            >
              科
            </span>
            <span>china-kekao</span>
          </NavLink>
          <nav className="hidden md:flex items-center gap-1">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground",
                    isActive && "bg-accent text-foreground",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            {user ? (
              <>
                <span className="text-sm text-muted-foreground hidden lg:inline">
                  {user.email}
                </span>
                <Button variant="outline" size="sm" onClick={handleLogout}>
                  Sign out
                </Button>
              </>
            ) : (
              <Button size="sm" onClick={() => navigate("/login")}>
                Sign in
              </Button>
            )}
          </div>
        </div>
        <nav className="md:hidden border-t">
          <div className="container flex items-center gap-1 overflow-x-auto py-2 -mx-1 px-1 no-scrollbar">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "shrink-0 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors",
                    isActive
                      ? "bg-foreground text-background border-foreground"
                      : "border-border text-muted-foreground hover:text-foreground",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </nav>
      </header>
      <main className="flex-1 container py-6">
        <Outlet />
      </main>
    </div>
  );
}
