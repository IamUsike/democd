import { useEffect, useState } from 'react';
import { getTransactions } from '../api/transactionsClient';
import { demoTransactions } from '../data/demoData';
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
        const filtered = demoTransactions.filter((txn) => {
          if (filters.sourceType && txn.sourceType !== filters.sourceType) {
            return false;
          }
          if (filters.sourceId && !txn.sourceId.includes(filters.sourceId)) {
            return false;
          }
          if (filters.accountId && !txn.accountId.includes(filters.accountId)) {
            return false;
          }
          return true;
        });
        setTransactions(filtered);
        setWarning('Using sample transaction data until backend APIs are available.');
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [filters.sourceType, filters.sourceId, filters.accountId]);

  return { transactions, loading, warning };
}

