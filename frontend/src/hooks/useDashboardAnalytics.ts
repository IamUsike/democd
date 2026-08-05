import { useState } from 'react';
import { getDashboardAnalytics } from '../api/dashboardClient';
import type { DashboardAnalytics } from '../types/dashboard';

export function useDashboardAnalytics() {
  const [analytics, setAnalytics] = useState<DashboardAnalytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [warning, setWarning] = useState<string | null>(null);
  const [lastCalculatedAt, setLastCalculatedAt] = useState<Date | null>(null);

  async function calculateGraphs() {
    setLoading(true);
    setWarning(null);

    try {
      const next = await getDashboardAnalytics();
      setAnalytics(next);
      setLastCalculatedAt(new Date());
    } catch {
      setWarning('Unable to calculate graphs from live data.');
    } finally {
      setLoading(false);
    }
  }

  return {
    analytics,
    loading,
    warning,
    lastCalculatedAt,
    calculateGraphs,
  };
}

