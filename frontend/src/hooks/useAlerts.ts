import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAlertById, getAlerts, updateAlertStatus } from '../api/alertsClient';
import type { Alert, AlertFilters, AlertStatus } from '../types/alert';
import { useDebouncedValue } from './useDebouncedValue';

export const ALL_STATUSES = ['ALL', 'OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED'] as const;
export type StatusFilter = (typeof ALL_STATUSES)[number];

const DEFAULT_SIZE = 50;

/**
 * Owns alert list filters, pagination, selection, and lifecycle updates.
 * List rows are summaries; selecting an id lazy-loads full detail.
 */
export function useAlerts(initialFilters: AlertFilters = {}) {
  const [filters, setFilters] = useState<AlertFilters>({
    status: '',
    severity: '',
    sourceType: '',
    sourceId: '',
    accountId: '',
    q: '',
    sort: 'createdAt,desc',
    page: 0,
    size: DEFAULT_SIZE,
    ...initialFilters,
  });

  // Debounce search so we don't hit the API on every keystroke.
  const debouncedQ = useDebouncedValue(filters.q ?? '', 300);

  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [selectedAlertId, setSelectedAlertId] = useState<number | null>(null);
  const [selectedAlert, setSelectedAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [warning, setWarning] = useState<string | null>(null);
  const [updating, setUpdating] = useState(false);

  // Strip empty strings so the API applies its own defaults (e.g. active statuses).
  const queryFilters = useMemo(
    () => ({
      sourceType: filters.sourceType || undefined,
      sourceId: filters.sourceId?.trim() || undefined,
      status: filters.status || undefined,
      severity: filters.severity || undefined,
      accountId: filters.accountId?.trim() || undefined,
      q: debouncedQ.trim() || undefined,
      sort: filters.sort || 'createdAt,desc',
      page: filters.page ?? 0,
      size: filters.size ?? DEFAULT_SIZE,
    }),
    [
      filters.sourceType,
      filters.sourceId,
      filters.status,
      filters.severity,
      filters.accountId,
      filters.sort,
      filters.page,
      filters.size,
      debouncedQ,
    ],
  );

  // Reload list whenever filters / page change. Cancel stale responses on rapid edits.
  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setWarning(null);
      try {
        const page = await getAlerts(queryFilters);
        if (cancelled) return;
        setAlerts(page.items);
        setTotalCount(page.totalCount);
        setHasNext(page.hasNext);
        if (page.items.length > 0) {
          // Keep selection if it still appears on this page; else select first row.
          setSelectedAlertId((prev) => {
            if (prev != null && page.items.some((a) => a.alertId === prev)) {
              return prev;
            }
            return page.items[0].alertId;
          });
        } else {
          setSelectedAlertId(null);
          setSelectedAlert(null);
        }
      } catch {
        if (cancelled) return;
        setAlerts([]);
        setTotalCount(0);
        setHasNext(false);
        setSelectedAlertId(null);
        setSelectedAlert(null);
        setWarning('Unable to load alerts from the API.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [queryFilters]);

  // Detail is a separate fetch (list omits linked transaction ids for payload size).
  useEffect(() => {
    if (selectedAlertId == null) {
      setSelectedAlert(null);
      return;
    }

    let cancelled = false;

    async function loadDetail() {
      setDetailLoading(true);
      try {
        const detail = await getAlertById(selectedAlertId!);
        if (cancelled) return;
        setSelectedAlert(detail);
        setAlerts((prev) =>
          prev.map((alert) => (alert.alertId === detail.alertId ? { ...alert, ...detail } : alert)),
        );
      } catch {
        if (!cancelled) {
          setWarning('Unable to load alert detail from the API.');
        }
      } finally {
        if (!cancelled) setDetailLoading(false);
      }
    }

    void loadDetail();
    return () => {
      cancelled = true;
    };
  }, [selectedAlertId]);

  const selectAlert = useCallback((alertId: number) => {
    setSelectedAlertId(alertId);
  }, []);

  const changeStatus = useCallback(
    async (alertId: number, nextStatus: AlertStatus, notes?: string) => {
      setUpdating(true);
      setWarning(null);
      try {
        const updated = await updateAlertStatus(alertId, { status: nextStatus, notes });
        setSelectedAlert((prev) => (prev?.alertId === alertId ? { ...prev, ...updated } : prev));
        setAlerts((prev) =>
          prev.map((alert) => (alert.alertId === alertId ? { ...alert, ...updated } : alert)),
        );
      } catch {
        setWarning('Unable to update alert status via the API.');
      } finally {
        setUpdating(false);
      }
    },
    [],
  );

  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, page }));
  }, []);

  return {
    alerts,
    selectedAlert,
    selectedAlertId,
    loading,
    detailLoading,
    warning,
    updating,
    filters,
    totalCount,
    hasNext,
    setFilters,
    setPage,
    selectAlert,
    changeStatus,
  };
}
