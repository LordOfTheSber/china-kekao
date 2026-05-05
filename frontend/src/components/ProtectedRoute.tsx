import { Navigate, useLocation } from "react-router-dom";
import type { PropsWithChildren } from "react";

import { useAuthStore } from "@/store/auth";

export function ProtectedRoute({ children }: PropsWithChildren) {
  const tokens = useAuthStore((s) => s.tokens);
  const location = useLocation();

  if (!tokens?.accessToken) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}
