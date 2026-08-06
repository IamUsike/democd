import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAlertById, getAlerts, updateAlertStatus } from '../api/alertsClient';
import type { Alert, AlertStatus } from '../types/alert';

export const ALL_STATUSES = ['ALL', 'OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED'] as const;
export type StatusFilter = typeof ALL_STATUSES[number];

export function useAlerts() {
  const [alerts, setAlerts]                   = useState<Alert[]>([]);
  const [selectedAlertId, setSelectedAlertId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter]       = useState<StatusFilter>('ALL');
  const [loading, setLoading]                 = useState(true);
  const [warning, setWarning]                 = useState<string | null>(null);
  const [updating, setUpdating]               = useState(false);

  /* Re-fetch whenever the status filter changes */
  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setWarning(null);
      setSelectedAlertId(null);
      try {
        const items = await getAlerts(statusFilter === 'ALL' ? undefined : statusFilter);
        if (!cancelled) {
          setAlerts(items);
          setSelectedAlertId(items[0]?.alertId ?? null);
        }
      } catch {
        if (!cancelled) {
          setAlerts([]);
          setSelectedAlertId(null);
          setWarning('Unable to load alerts from the API.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => { cancelled = true; };
  }, [statusFilter]);

  const selectedAlert = useMemo(
    () => alerts.find((a) => a.alertId === selectedAlertId) ?? null,
    [alerts, selectedAlertId],
  );

  /** Select an alert and hydrate its detail fields from the API. */
  const selectAlert = useCallback(async (alertId: number) => {
    setSelectedAlertId(alertId);
    try {
      const detail = await getAlertById(alertId);
      setAlerts((prev) =>
        prev.map((a) => (a.alertId === alertId ? { ...a, ...detail } : a)),
      );
    } catch {
      setWarning('Unable to load alert detail from the API.');
    }
  }, []);

  /** Transition an alert's lifecycle status. */
  const changeStatus = useCallback(
    async (alertId: number, nextStatus: AlertStatus, notes?: string) => {
      setUpdating(true);
      setWarning(null);
      try {
        const updated = await updateAlertStatus(alertId, { status: nextStatus, notes });
        setAlerts((prev) =>
          prev.map((a) => (a.alertId === alertId ? { ...a, ...updated } : a)),
        );
      } catch {
        setWarning('Unable to update alert status via the API.');
      } finally {
        setUpdating(false);
      }
    },
    [],
  );

  return {
    alerts,
    selectedAlert,
    selectedAlertId,
    statusFilter,
    setStatusFilter,
    loading,
    warning,
    updating,
    selectAlert,
    changeStatus,
  };
}
