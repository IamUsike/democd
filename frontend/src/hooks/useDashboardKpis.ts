import { useEffect, useState } from 'react';
import { getDashboardSummary } from '../api/dashboardClient';
import type { DashboardSummary } from '../types/dashboard';

const emptySummary: DashboardSummary = {
  totalTransactions: 0,
  totalAlerts: 0,
  openAlerts: 0,
  closedAlerts: 0,
  highSeverityAlerts: 0,
};

export function useDashboardKpis() {
  const [summary, setSummary] = useState<DashboardSummary>(emptySummary);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setWarning(null);
      try {
        const liveData = await getDashboardSummary();
        setSummary(liveData);
      } catch {
        setSummary(emptySummary);
        setWarning('Unable to load dashboard KPIs from the API.');
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  return { summary, loading, warning };
}
