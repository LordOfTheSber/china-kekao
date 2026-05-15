import { useEffect } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { PaperBg } from "@/components/ui/paper-bg";
import { Seal } from "@/components/ui/seal";
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
  const location = useLocation();
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
    <div className="min-h-dvh flex flex-col bg-paper text-ink">
      <PaperBg />
      <header className="sticky top-0 z-30 border-b border-brush/40 bg-paper/85 backdrop-blur supports-[backdrop-filter]:bg-paper/65">
        <div className="container flex h-14 items-center justify-between gap-3">
          <NavLink
            to="/"
            className="flex items-center gap-2.5 font-semibold tracking-tight min-w-0"
          >
            <Seal size="sm" tilt={false} className="font-hanzi">
              科
            </Seal>
            <span className="hidden xs:inline truncate font-hanzi text-base">
              china <span className="text-ink-soft">·</span> kekao
            </span>
          </NavLink>
          <nav className="hidden md:flex items-center gap-1">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "relative px-3 py-1.5 text-sm font-medium text-ink-soft transition-colors hover:text-ink",
                    "after:absolute after:left-2 after:right-2 after:-bottom-0.5 after:h-[3px] after:rounded-full after:bg-seal after:scale-x-0 after:transition-transform",
                    isActive && "text-ink after:scale-x-100",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-2 min-w-0">
            {user ? (
              <>
                <span className="text-sm text-muted-foreground hidden lg:inline max-w-[18ch] truncate">
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
        <nav className="md:hidden border-t border-brush/40 bg-paper/60">
          <div className="container flex items-center gap-1.5 overflow-x-auto py-2 no-scrollbar">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "shrink-0 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors",
                    isActive
                      ? "bg-seal text-seal-foreground border-seal shadow-seal"
                      : "border-brush/40 text-ink-soft hover:text-ink hover:bg-accent/40",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </nav>
      </header>
      <main
        key={location.pathname}
        className="flex-1 container py-6 min-w-0 safe-pad-b animate-brush-wipe motion-reduce:animate-none"
      >
        <Outlet />
      </main>
    </div>
  );
}
