import { Route, Routes } from "react-router-dom";

import "@/api/client";
import { Layout } from "@/components/Layout";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { LoginPage } from "@/pages/Login";
import { RegisterPage } from "@/pages/Register";
import { DashboardPage } from "@/pages/Dashboard";
import { StudyPage } from "@/pages/Study";
import { DecksPage } from "@/pages/Decks";
import { DeckEditPage } from "@/pages/DeckEdit";
import { HanziDetailPage } from "@/pages/HanziDetail";
import { SearchPage } from "@/pages/Search";
import { StatsPage } from "@/pages/Stats";
import { SettingsPage } from "@/pages/Settings";
import { NotFoundPage } from "@/pages/NotFound";

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          index
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/study"
          element={
            <ProtectedRoute>
              <StudyPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/decks"
          element={
            <ProtectedRoute>
              <DecksPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/decks/:id"
          element={
            <ProtectedRoute>
              <DeckEditPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/hanzi/:id"
          element={
            <ProtectedRoute>
              <HanziDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/search"
          element={
            <ProtectedRoute>
              <SearchPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/stats"
          element={
            <ProtectedRoute>
              <StatsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <SettingsPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
