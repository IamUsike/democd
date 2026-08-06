import { useCallback, useEffect, useRef, useState } from 'react';
import { getTransactions } from '../api/transactionsClient';
import type { Transaction, TransactionFilters } from '../types/transaction';

type UseTransactionsOptions = {
  autoRefresh: boolean;
  paused: boolean;
  refreshIntervalMs?: number;
};

export function useTransactions(
  filters: TransactionFilters,
  options: UseTransactionsOptions,
) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const [newTransactionIds, setNewTransactionIds] = useState<number[]>([]);
  const [refreshToken, setRefreshToken] = useState(0);
  const previousIdsRef = useRef<Set<number>>(new Set());

  const refreshNow = useCallback(() => {
    setRefreshToken((value) => value + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setWarning(null);
      try {
        const items = await getTransactions(filters);
        if (cancelled) {
          return;
        }

        const previousIds = previousIdsRef.current;
        const currentIds = new Set(items.map((txn) => txn.transactionId));
        const addedIds = previousIds.size === 0
          ? []
          : items
              .filter((txn) => !previousIds.has(txn.transactionId))
              .map((txn) => txn.transactionId);

        setTransactions(items);
        setNewTransactionIds(addedIds);
        setLastUpdatedAt(new Date());
        previousIdsRef.current = currentIds;
      } catch {
        if (cancelled) {
          return;
        }
        setTransactions([]);
        setNewTransactionIds([]);
        setWarning('Unable to load transactions from the API.');
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    if (!options.autoRefresh || options.paused) {
      return () => {
        cancelled = true;
      };
    }

    const intervalId = window.setInterval(() => {
      void load();
    }, options.refreshIntervalMs ?? 3000);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [
    filters.sourceType,
    filters.sourceId,
    filters.accountId,
    options.autoRefresh,
    options.paused,
    options.refreshIntervalMs,
    refreshToken,
  ]);

  return {
    transactions,
    loading,
    warning,
    lastUpdatedAt,
    newTransactionIds,
    refreshNow,
  };
}
