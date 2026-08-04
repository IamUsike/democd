import { useEffect, useState } from 'react';
import { getDashboardSummary } from '../api/dashboardClient';
import { demoDashboardSummary } from '../data/demoData';
import type { DashboardSummary } from '../types/dashboard';

export function useDashboardKpis() {
  const [summary, setSummary] = useState<DashboardSummary>(demoDashboardSummary);
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
        setWarning('Using sample KPI data until backend dashboard APIs are available.');
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  return { summary, loading, warning };
}

