import type { ApiEnvelope } from '../types/api';

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.toString() ?? 'http://localhost:8081';

export async function apiGet<T>(path: string): Promise<ApiEnvelope<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`GET ${path} failed with status ${response.status}`);
  }

  return (await response.json()) as ApiEnvelope<T>;
}

export async function apiPatch<TRequest, TResponse>(
  path: string,
  body: TRequest,
): Promise<ApiEnvelope<TResponse>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`PATCH ${path} failed with status ${response.status}`);
  }

  return (await response.json()) as ApiEnvelope<TResponse>;
}

export { API_BASE_URL };

