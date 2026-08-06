import { apiGet } from './http';
import type { PageResponse } from '../types/page';
import type { Transaction, TransactionFilters } from '../types/transaction';

type TransactionQuery = TransactionFilters & {
  afterId?: number;
};

function toQueryString(filters: TransactionQuery): string {
  const params = new URLSearchParams();

  if (filters.sourceType) params.set('sourceType', filters.sourceType);
  if (filters.sourceId) params.set('sourceId', filters.sourceId);
  if (filters.accountId) params.set('accountId', filters.accountId);
  if (filters.q) params.set('q', filters.q);
  if (filters.sort) params.set('sort', filters.sort);
  if (filters.page != null) params.set('page', String(filters.page));
  if (filters.size != null) params.set('size', String(filters.size));
  if (filters.afterId != null) params.set('afterId', String(filters.afterId));

  const query = params.toString();
  return query ? `?${query}` : '';
}

export async function getTransactions(
  filters: TransactionQuery = {},
): Promise<PageResponse<Transaction>> {
  const envelope = await apiGet<PageResponse<Transaction>>(
    `/api/v1/transactions${toQueryString(filters)}`,
  );
  return envelope.data;
}
