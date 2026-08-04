import { apiGet, apiPatch } from './http';
import type { Alert, AlertStatusUpdateRequest } from '../types/alert';

export async function getAlerts(): Promise<Alert[]> {
  const envelope = await apiGet<Alert[]>('/api/v1/alerts');
  return envelope.data;
}

export async function getAlertById(alertId: number): Promise<Alert> {
  const envelope = await apiGet<Alert>(`/api/v1/alerts/${alertId}`);
  return envelope.data;
}

export async function updateAlertStatus(
  alertId: number,
  payload: AlertStatusUpdateRequest,
): Promise<void> {
  await apiPatch<AlertStatusUpdateRequest, unknown>(
    `/api/v1/alerts/${alertId}/status`,
    payload,
  );
}

