import { useMemo, useRef } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import type { Transaction, TransactionFilters } from '../types/transaction';
import { StatusIndicator } from './StatusIndicator';

type TransactionsPanelProps = {
  transactions: Transaction[];
  filters: TransactionFilters;
  pinnedTransactionId: string;
  autoRefresh: boolean;
  paused: boolean;
  lastUpdatedAt: Date | null;
  newTransactionIds: number[];
  loading: boolean;
  totalCount: number;
  hasNext: boolean;
  onFilterChange: (nextFilters: TransactionFilters) => void;
  onPinnedTransactionIdChange: (value: string) => void;
  onAutoRefreshChange: (enabled: boolean) => void;
  onPausedChange: (paused: boolean) => void;
  onRefreshNow: () => void;
  onPageChange: (page: number) => void;
};

export function TransactionsPanel({
  transactions,
  filters,
  pinnedTransactionId,
  autoRefresh,
  paused,
  lastUpdatedAt,
  newTransactionIds,
  loading,
  totalCount,
  hasNext,
  onFilterChange,
  onPinnedTransactionIdChange,
  onAutoRefreshChange,
  onPausedChange,
  onRefreshNow,
  onPageChange,
}: TransactionsPanelProps) {
  const pinnedId = Number(pinnedTransactionId);
  const newIdsSet = useMemo(() => new Set(newTransactionIds), [newTransactionIds]);
  const page = filters.page ?? 0;

  const visibleTransactions = useMemo(() => {
    if (!Number.isFinite(pinnedId)) {
      return transactions;
    }
    const pinnedRow = transactions.find((txn) => txn.transactionId === pinnedId);
    if (!pinnedRow) {
      return transactions;
    }
    return [pinnedRow, ...transactions.filter((txn) => txn.transactionId !== pinnedId)];
  }, [transactions, pinnedId]);

  const parentRef = useRef<HTMLDivElement>(null);
  const rowVirtualizer = useVirtualizer({
    count: loading && visibleTransactions.length === 0 ? 10 : visibleTransactions.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 44,
    overscan: 10,
  });

  return (
    <section className="card">
      <header className="section-header">
        <h2>Transactions</h2>
        <span className="muted">
          {loading ? 'Loading...' : `${totalCount.toLocaleString()} total`} ·{' '}
          {lastUpdatedAt ? `updated ${lastUpdatedAt.toLocaleTimeString()}` : 'not loaded yet'}
        </span>
      </header>

      <div className="feed-controls">
        <label className="inline-toggle">
          <input
            type="checkbox"
            checked={autoRefresh}
            onChange={(event) => onAutoRefreshChange(event.target.checked)}
          />
          Auto-refresh new rows (3s)
        </label>

        <button type="button" className="feed-button" onClick={() => onPausedChange(!paused)}>
          {paused ? 'Resume feed' : 'Pause feed'}
        </button>

        <button type="button" className="feed-button" onClick={onRefreshNow}>
          Refresh page
        </button>

        <input
          className="feed-input"
          value={filters.q ?? ''}
          onChange={(event) => onFilterChange({ ...filters, q: event.target.value, page: 0 })}
          placeholder="Search ID/source/account/payee (debounced)"
        />

        <input
          className="feed-input feed-input-small"
          value={pinnedTransactionId}
          onChange={(event) => onPinnedTransactionIdChange(event.target.value)}
          placeholder="Pin ID"
        />
      </div>

      <div className="filters">
        <label>
          Source Type
          <select
            value={filters.sourceType ?? ''}
            onChange={(event) =>
              onFilterChange({
                ...filters,
                sourceType: event.target.value as TransactionFilters['sourceType'],
                page: 0,
              })
            }
          >
            <option value="">All</option>
            <option value="BANK">BANK</option>
            <option value="MERCHANT">MERCHANT</option>
          </select>
        </label>

        <label>
          Source ID
          <input
            value={filters.sourceId ?? ''}
            onChange={(event) =>
              onFilterChange({ ...filters, sourceId: event.target.value, page: 0 })
            }
            placeholder="HSBC-UK"
          />
        </label>

        <label>
          Account ID
          <input
            value={filters.accountId ?? ''}
            onChange={(event) =>
              onFilterChange({ ...filters, accountId: event.target.value, page: 0 })
            }
            placeholder="ACC1001"
          />
        </label>

        <label>
          Sort
          <select
            value={filters.sort ?? 'timestamp,desc'}
            onChange={(event) =>
              onFilterChange({ ...filters, sort: event.target.value, page: 0 })
            }
          >
            <option value="timestamp,desc">Newest first</option>
            <option value="timestamp,asc">Oldest first</option>
            <option value="amount,desc">Amount high → low</option>
            <option value="transactionId,desc">ID desc</option>
          </select>
        </label>
      </div>

      <div className="table-wrap virtual-table-wrap" ref={parentRef}>
        <table className="virtual-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Source</th>
              <th>Account</th>
              <th>Payee</th>
              <th>Amount</th>
              <th>Type</th>
              <th>Status</th>
              <th>Time</th>
            </tr>
          </thead>
        </table>
        <div
          className="virtual-list-inner"
          style={{ height: `${rowVirtualizer.getTotalSize()}px`, position: 'relative' }}
        >
          {loading && visibleTransactions.length === 0
            ? rowVirtualizer.getVirtualItems().map((virtualRow) => (
                <div
                  key={virtualRow.key}
                  className="skeleton-row table-skeleton"
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: `${virtualRow.size}px`,
                    transform: `translateY(${virtualRow.start}px)`,
                  }}
                />
              ))
            : rowVirtualizer.getVirtualItems().map((virtualRow) => {
                const txn = visibleTransactions[virtualRow.index];
                if (!txn) return null;
                return (
                  <div
                    key={txn.transactionId}
                    className={`virtual-table-row ${newIdsSet.has(txn.transactionId) ? 'row-new' : ''} ${
                      Number.isFinite(pinnedId) && txn.transactionId === pinnedId ? 'row-pinned' : ''
                    }`}
                    style={{
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      width: '100%',
                      height: `${virtualRow.size}px`,
                      transform: `translateY(${virtualRow.start}px)`,
                    }}
                  >
                    <span className="cell-data">{txn.transactionId}</span>
                    <span className="source-label">
                      {txn.sourceType} {txn.sourceName}
                    </span>
                    <span className="cell-data">{txn.accountId}</span>
                    <span>{txn.payeeName ?? txn.payeeId}</span>
                    <span className="cell-amount">
                      {txn.currency} {txn.amount.toLocaleString()}
                    </span>
                    <span>{txn.transactionType}</span>
                    <span>
                      <StatusIndicator status={txn.status} />
                    </span>
                    <span className="cell-data">{new Date(txn.timestamp).toLocaleString()}</span>
                  </div>
                );
              })}
        </div>
        {!loading && visibleTransactions.length === 0 && (
          <p className="muted empty-row">No transactions found.</p>
        )}
      </div>

      <div className="pager">
        <button
          type="button"
          className="feed-button"
          disabled={page <= 0 || loading}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <span className="muted">Page {page + 1}</span>
        <button
          type="button"
          className="feed-button"
          disabled={!hasNext || loading}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}
