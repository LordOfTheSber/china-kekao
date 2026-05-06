import { api } from "@/api/client";
import type { DashboardStats } from "@/api/types";

export async function fetchDashboard(): Promise<DashboardStats> {
  const response = await api.get<DashboardStats>("/stats/dashboard");
  return response.data;
}

export interface DailyReview {
  date: string;
  total: number;
  good: number;
  accuracy: number;
}

export interface CardStateBreakdown {
  newCount: number;
  learning: number;
  review: number;
  relearning: number;
}

export interface OverviewView {
  days: number;
  daily: DailyReview[];
  states: CardStateBreakdown;
}

export async function fetchOverview(days = 30): Promise<OverviewView> {
  const response = await api.get<OverviewView>("/stats/overview", {
    params: { days },
  });
  return response.data;
}
