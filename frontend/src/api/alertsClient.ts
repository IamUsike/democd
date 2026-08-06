import { apiGet, apiPatch } from './http';
import type { Alert, AlertStatusUpdateRequest } from '../types/alert';

/**
 * Fetch alerts, optionally filtered by status.
 * Maps to: GET /api/v1/alerts?status={status}
 * Passing no status (or 'ALL') returns all alerts.
 */
export async function getAlerts(status?: string): Promise<Alert[]> {
  const query = status && status !== 'ALL' ? `?status=${encodeURIComponent(status)}` : '';
  const envelope = await apiGet<Alert[]>(`/api/v1/alerts${query}`);
  return envelope.data;
}

export async function getAlertById(alertId: number): Promise<Alert> {
  const envelope = await apiGet<Alert>(`/api/v1/alerts/${alertId}`);
  return envelope.data;
}

export async function updateAlertStatus(
  alertId: number,
  payload: AlertStatusUpdateRequest,
): Promise<Alert> {
  const envelope = await apiPatch<AlertStatusUpdateRequest, Alert>(
    `/api/v1/alerts/${alertId}/status`,
    payload,
  );
  return envelope.data;
}
