import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { PaperBg } from "@/components/ui/paper-bg";
import { Seal } from "@/components/ui/seal";
import { AchievementUnlock } from "@/components/AchievementUnlock";
import { IsekaiTruck } from "@/components/IsekaiTruck";
import { PandaEgg } from "@/components/PandaEgg";
import { useAuthStore } from "@/store/auth";
import { usePreferencesStore } from "@/store/preferences";
import { useThemeStore } from "@/store/theme";
import { fetchUserSettings } from "@/api/me";
import { claimAchievement, type AchievementView } from "@/api/achievements";
import { useKonamiCode } from "@/lib/easter-eggs";
import { toast } from "@/components/Toaster";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/study", label: "Study" },
  { to: "/decks", label: "Decks" },
  { to: "/search", label: "Search" },
  { to: "/stats", label: "Stats" },
  { to: "/achievements", label: "Seals" },
  { to: "/settings", label: "Settings" },
];

export function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const hydrate = usePreferencesStore((s) => s.hydrate);
  const setTheme = useThemeStore((s) => s.setTheme);
  const themeId = useThemeStore((s) => s.themeId);
  const [unlocks, setUnlocks] = useState<AchievementView[]>([]);
  const [pandaOpen, setPandaOpen] = useState(false);
  const logoClicksRef = useRef(0);
  const logoTimerRef = useRef<number | null>(null);

  useKonamiCode((achievement) => {
    setTheme("arcade");
    toast({
      title: "Arcade mode unlocked",
      description: "Try the new theme in Settings → Appearance.",
    });
    if (achievement) setUnlocks((prev) => [...prev, achievement]);
  });

  function handleLogoClick() {
    logoClicksRef.current += 1;
    if (logoTimerRef.current) window.clearTimeout(logoTimerRef.current);
    logoTimerRef.current = window.setTimeout(() => {
      logoClicksRef.current = 0;
    }, 2500);
    if (logoClicksRef.current >= 10) {
      logoClicksRef.current = 0;
      setPandaOpen(true);
      claimAchievement("PANDA")
        .then((r) => {
          if (r.unlocked) {
            const ach = (r as unknown as { achievement: AchievementView }).achievement;
            setUnlocks((prev) => [...prev, ach]);
          }
        })
        .catch(() => undefined);
    }
  }

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
    <div className={`min-h-dvh flex flex-col text-ink ${themeId === "anime-isekai" ? "" : "bg-paper"}`}>
      <PaperBg />
      <header className="sticky top-0 z-30 border-b border-brush/40 bg-paper/85 backdrop-blur supports-[backdrop-filter]:bg-paper/65">
        <div className="container flex h-14 items-center justify-between gap-3">
          <NavLink
            to="/"
            onClick={handleLogoClick}
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
      {themeId === "anime-isekai" && (
        <>
          {/* Hero anime girl — place asset at frontend/public/anime-isekai/hero-girl.png (see plan for prompt). */}
          <img
            src="/anime-isekai/hero-girl.png"
            alt=""
            aria-hidden
            className="pointer-events-none fixed bottom-0 right-2 hidden lg:block h-[70vh] opacity-90 z-10 drop-shadow-[0_0_30px_hsl(var(--seal)/0.4)]"
          />
          <div className="pointer-events-none fixed bottom-4 left-0 z-10">
            <IsekaiTruck />
          </div>
          {/* Decorative stamp — place asset at frontend/public/anime-isekai/seal-stamp.png (see plan for prompt). */}
          <img
            src="/anime-isekai/seal-stamp.png"
            alt=""
            aria-hidden
            className="pointer-events-none fixed top-20 right-4 hidden md:block h-24 w-24 opacity-80 -rotate-12 z-10 drop-shadow-[0_0_18px_hsl(var(--seal)/0.5)]"
          />
        </>
      )}
      <AchievementUnlock unlocks={unlocks} onDone={() => setUnlocks([])} />
      <PandaEgg open={pandaOpen} onClose={() => setPandaOpen(false)} />
    </div>
  );
}
