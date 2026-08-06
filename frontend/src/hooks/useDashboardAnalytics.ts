import { useState } from 'react';
import { getDashboardAnalytics, getDemoDashboardAnalytics } from '../api/dashboardClient';
import type { DashboardAnalytics } from '../types/dashboard';

type AnalyticsSource = 'LIVE' | 'DEMO';

export function useDashboardAnalytics() {
  const [analytics, setAnalytics] = useState<DashboardAnalytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [warning, setWarning] = useState<string | null>(null);
  const [infoMessage, setInfoMessage] = useState<string | null>(null);
  const [lastCalculatedAt, setLastCalculatedAt] = useState<Date | null>(null);
  const [source, setSource] = useState<AnalyticsSource | null>(null);

  async function calculateGraphs() {
    setLoading(true);
    setWarning(null);
    setInfoMessage(null);

    try {
      const next = await getDashboardAnalytics();
      setAnalytics(next);
      setLastCalculatedAt(new Date());
      setSource('LIVE');
    } catch {
      setWarning('Unable to calculate graphs from live data. Try Show Demo.');
    } finally {
      setLoading(false);
    }
  }

  function calculateDemoGraphs() {
    setWarning(null);
    setInfoMessage('Showing demo graph preview data.');
    setAnalytics(getDemoDashboardAnalytics());
    setLastCalculatedAt(new Date());
    setSource('DEMO');
  }

  return {
    analytics,
    loading,
    warning,
    infoMessage,
    lastCalculatedAt,
    source,
    calculateGraphs,
    calculateDemoGraphs,
  };
}
