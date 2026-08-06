import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getTransactions } from '../api/transactionsClient';
import type { Transaction, TransactionFilters } from '../types/transaction';
import { useDebouncedValue } from './useDebouncedValue';

type UseTransactionsOptions = {
  autoRefresh: boolean;
  paused: boolean;
  refreshIntervalMs?: number;
};

const DEFAULT_SIZE = 50;
/** Cap in-memory rows when delta-polling so the feed cannot grow without bound. */
const MAX_FEED_BUFFER = 200;

/**
 * Paginated transaction browse + optional afterId delta poll for new rows.
 * Full reload on filter/page change; poll only appends newer ids.
 */
export function useTransactions(
  filters: TransactionFilters,
  options: UseTransactionsOptions,
) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const [newTransactionIds, setNewTransactionIds] = useState<number[]>([]);
  const [refreshToken, setRefreshToken] = useState(0);
  const maxIdRef = useRef(0);

  const debouncedQ = useDebouncedValue(filters.q ?? '', 300);
  const page = filters.page ?? 0;
  const size = filters.size ?? DEFAULT_SIZE;

  // Shared query for page loads and delta polls (poll overrides page/sort/afterId).
  const baseQuery = useMemo(
    () => ({
      sourceType: filters.sourceType || undefined,
      sourceId: filters.sourceId?.trim() || undefined,
      accountId: filters.accountId?.trim() || undefined,
      q: debouncedQ.trim() || undefined,
      sort: filters.sort || 'timestamp,desc',
      page,
      size,
    }),
    [
      filters.sourceType,
      filters.sourceId,
      filters.accountId,
      filters.sort,
      debouncedQ,
      page,
      size,
    ],
  );

  const refreshNow = useCallback(() => {
    setRefreshToken((value) => value + 1);
  }, []);

  // Full page load when filters, page, or manual refresh token change.
  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setWarning(null);
      try {
        const result = await getTransactions(baseQuery);
        if (cancelled) return;

        setTransactions(result.items);
        setTotalCount(result.totalCount);
        setHasNext(result.hasNext);
        setNewTransactionIds([]);
        setLastUpdatedAt(new Date());
        maxIdRef.current = result.items.reduce(
          (max, txn) => Math.max(max, txn.transactionId),
          0,
        );
      } catch {
        if (cancelled) return;
        setTransactions([]);
        setTotalCount(0);
        setHasNext(false);
        setNewTransactionIds([]);
        setWarning('Unable to load transactions from the API.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [baseQuery, refreshToken]);

  // Delta poll: only rows with transactionId > max seen so far.
  useEffect(() => {
    if (!options.autoRefresh || options.paused) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      void (async () => {
        const afterId = maxIdRef.current;
        if (afterId <= 0) return;

        try {
          const result = await getTransactions({
            ...baseQuery,
            page: 0,
            afterId,
            sort: 'transactionId,asc',
          });
          if (result.items.length === 0) {
            setLastUpdatedAt(new Date());
            return;
          }

          const addedIds = result.items.map((txn) => txn.transactionId);
          setNewTransactionIds(addedIds);
          setTransactions((prev) => {
            const merged = [...result.items.slice().reverse(), ...prev];
            const deduped: Transaction[] = [];
            const seen = new Set<number>();
            for (const txn of merged) {
              if (seen.has(txn.transactionId)) continue;
              seen.add(txn.transactionId);
              deduped.push(txn);
            }
            return deduped.slice(0, MAX_FEED_BUFFER);
          });
          setTotalCount((prev) => prev + result.items.length);
          maxIdRef.current = Math.max(
            maxIdRef.current,
            ...result.items.map((txn) => txn.transactionId),
          );
          setLastUpdatedAt(new Date());
        } catch {
          setWarning('Unable to poll new transactions from the API.');
        }
      })();
    }, options.refreshIntervalMs ?? 3000);

    return () => window.clearInterval(intervalId);
  }, [options.autoRefresh, options.paused, options.refreshIntervalMs, baseQuery]);

  return {
    transactions,
    totalCount,
    hasNext,
    loading,
    warning,
    lastUpdatedAt,
    newTransactionIds,
    refreshNow,
  };
}
