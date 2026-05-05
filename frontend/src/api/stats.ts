import { api } from "@/api/client";
import type { DashboardStats } from "@/api/types";

export async function fetchDashboard(): Promise<DashboardStats> {
  const response = await api.get<DashboardStats>("/stats/dashboard");
  return response.data;
}
