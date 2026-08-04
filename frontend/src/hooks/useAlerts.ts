import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAlertById, getAlerts, updateAlertStatus } from '../api/alertsClient';
import { demoAlerts } from '../data/demoData';
import type { Alert, AlertStatus } from '../types/alert';

function withUpdatedStatus(alert: Alert, nextStatus: AlertStatus, notes?: string): Alert {
  const now = new Date().toISOString();

  return {
    ...alert,
    status: nextStatus,
    resolutionNotes: notes ?? alert.resolutionNotes,
    acknowledgedAt: nextStatus === 'ACKNOWLEDGED' ? now : alert.acknowledgedAt,
    investigatingAt: nextStatus === 'INVESTIGATING' ? now : alert.investigatingAt,
    dismissedAt: nextStatus === 'DISMISSED' ? now : alert.dismissedAt,
    closedAt: nextStatus === 'CLOSED' ? now : alert.closedAt,
  };
}

export function useAlerts() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [selectedAlertId, setSelectedAlertId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState<string | null>(null);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setWarning(null);
      try {
        const items = await getAlerts();
        setAlerts(items);
        setSelectedAlertId(items[0]?.alertId ?? null);
      } catch {
        setAlerts(demoAlerts);
        setSelectedAlertId(demoAlerts[0]?.alertId ?? null);
        setWarning('Using sample alerts until backend APIs are available.');
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  const selectedAlert = useMemo(
    () => alerts.find((alert) => alert.alertId === selectedAlertId) ?? null,
    [alerts, selectedAlertId],
  );

  const selectAlert = useCallback(async (alertId: number) => {
    setSelectedAlertId(alertId);
    try {
      const detail = await getAlertById(alertId);
      setAlerts((prev) =>
        prev.map((alert) => (alert.alertId === alertId ? { ...alert, ...detail } : alert)),
      );
    } catch {
      // Ignore detail fetch errors because list data is enough for MVP UI.
    }
  }, []);

  const changeStatus = useCallback(
    async (alertId: number, nextStatus: AlertStatus, notes?: string) => {
      setUpdating(true);
      try {
        await updateAlertStatus(alertId, { status: nextStatus, notes });
      } catch {
        setWarning('Status update queued in UI only because backend endpoint is unavailable.');
      } finally {
        setAlerts((prev) =>
          prev.map((alert) =>
            alert.alertId === alertId ? withUpdatedStatus(alert, nextStatus, notes) : alert,
          ),
        );
        setUpdating(false);
      }
    },
    [],
  );

  return {
    alerts,
    selectedAlert,
    selectedAlertId,
    loading,
    warning,
    updating,
    selectAlert,
    changeStatus,
  };
}

