import { apiGet } from './http';
import type { DashboardSummary } from '../types/dashboard';

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const envelope = await apiGet<DashboardSummary>('/api/v1/dashboard');
  return envelope.data;
}

