import { apiGet, apiPatch } from './http';
import type { Alert, AlertFilters, AlertStatusUpdateRequest } from '../types/alert';
import type { PageResponse } from '../types/page';

/** Build query string; omit empty values so the API can apply defaults. */
function toQueryString(filters: AlertFilters): string {
  const params = new URLSearchParams();

  if (filters.sourceType) params.set('sourceType', filters.sourceType);
  if (filters.sourceId) params.set('sourceId', filters.sourceId);
  if (filters.status) params.set('status', filters.status);
  if (filters.severity) params.set('severity', filters.severity);
  if (filters.accountId) params.set('accountId', filters.accountId);
  if (filters.q) params.set('q', filters.q);
  if (filters.sort) params.set('sort', filters.sort);
  if (filters.page != null) params.set('page', String(filters.page));
  if (filters.size != null) params.set('size', String(filters.size));

  const query = params.toString();
  return query ? `?${query}` : '';
}

/**
 * Fetch a page of alerts with optional filters.
 * Omitting status → API default (active: OPEN | ACKNOWLEDGED | INVESTIGATING).
 * Pass status=ALL for full history.
 */
export async function getAlerts(filters: AlertFilters = {}): Promise<PageResponse<Alert>> {
  const envelope = await apiGet<PageResponse<Alert>>(`/api/v1/alerts${toQueryString(filters)}`);
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
