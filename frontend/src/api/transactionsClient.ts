import { apiGet } from './http';
import type { Transaction, TransactionFilters } from '../types/transaction';

function toQueryString(filters: TransactionFilters): string {
  const params = new URLSearchParams();

  if (filters.sourceType) params.set('sourceType', filters.sourceType);
  if (filters.sourceId) params.set('sourceId', filters.sourceId);
  if (filters.accountId) params.set('accountId', filters.accountId);

  const query = params.toString();
  return query ? `?${query}` : '';
}

export async function getTransactions(
  filters: TransactionFilters,
): Promise<Transaction[]> {
  const envelope = await apiGet<Transaction[]>(
    `/api/v1/transactions${toQueryString(filters)}`,
  );
  return envelope.data;
}

