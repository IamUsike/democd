import { useEffect, useState } from 'react';
import { getTransactions } from '../api/transactionsClient';
import type { Transaction, TransactionFilters } from '../types/transaction';

export function useTransactions(filters: TransactionFilters) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setWarning(null);
      try {
        const items = await getTransactions(filters);
        setTransactions(items);
      } catch {
        setTransactions([]);
        setWarning('Unable to load transactions from the API.');
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [filters.sourceType, filters.sourceId, filters.accountId]);

  return { transactions, loading, warning };
}
