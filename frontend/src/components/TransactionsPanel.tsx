import { useMemo } from 'react';
import type { Transaction, TransactionFilters } from '../types/transaction';
import { StatusIndicator } from './StatusIndicator';

type TransactionsPanelProps = {
  transactions: Transaction[];
  filters: TransactionFilters;
  quickSearch: string;
  pinnedTransactionId: string;
  autoRefresh: boolean;
  paused: boolean;
  lastUpdatedAt: Date | null;
  newTransactionIds: number[];
  loading: boolean;
  onFilterChange: (nextFilters: TransactionFilters) => void;
  onQuickSearchChange: (value: string) => void;
  onPinnedTransactionIdChange: (value: string) => void;
  onAutoRefreshChange: (enabled: boolean) => void;
  onPausedChange: (paused: boolean) => void;
  onRefreshNow: () => void;
};

export function TransactionsPanel({
  transactions,
  filters,
  quickSearch,
  pinnedTransactionId,
  autoRefresh,
  paused,
  lastUpdatedAt,
  newTransactionIds,
  loading,
  onFilterChange,
  onQuickSearchChange,
  onPinnedTransactionIdChange,
  onAutoRefreshChange,
  onPausedChange,
  onRefreshNow,
}: TransactionsPanelProps) {
  const normalizedSearch = quickSearch.trim().toLowerCase();
  const pinnedId = Number(pinnedTransactionId);
  const newIdsSet = useMemo(() => new Set(newTransactionIds), [newTransactionIds]);

  const visibleTransactions = useMemo(() => {
    const filtered = normalizedSearch
      ? transactions.filter((txn) => {
          const searchable = [
            String(txn.transactionId),
            txn.sourceName,
            txn.sourceId,
            txn.accountId,
            txn.payeeId,
            txn.payeeName ?? '',
            txn.description ?? '',
          ]
            .join(' ')
            .toLowerCase();
          return searchable.includes(normalizedSearch);
        })
      : transactions;

    if (!Number.isFinite(pinnedId)) {
      return filtered;
    }

    const pinnedRow = filtered.find((txn) => txn.transactionId === pinnedId);
    if (!pinnedRow) {
      return filtered;
    }

    return [pinnedRow, ...filtered.filter((txn) => txn.transactionId !== pinnedId)];
  }, [transactions, normalizedSearch, pinnedId]);

  return (
    <section className="card">
      <header className="section-header">
        <h2>Transactions</h2>
        <span className="muted">
          {loading ? 'Loading...' : `${visibleTransactions.length} shown`} ·{' '}
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
          Auto-refresh (3s)
        </label>

        <button
          type="button"
          className="feed-button"
          onClick={() => onPausedChange(!paused)}
        >
          {paused ? 'Resume feed' : 'Pause feed'}
        </button>

        <button type="button" className="feed-button" onClick={onRefreshNow}>
          Refresh now
        </button>

        <input
          className="feed-input"
          value={quickSearch}
          onChange={(event) => onQuickSearchChange(event.target.value)}
          placeholder="Quick search by ID/source/account/payee"
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
              onFilterChange({ ...filters, sourceType: event.target.value as TransactionFilters['sourceType'] })
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
            onChange={(event) => onFilterChange({ ...filters, sourceId: event.target.value })}
            placeholder="HSBC-UK"
          />
        </label>

        <label>
          Account ID
          <input
            value={filters.accountId ?? ''}
            onChange={(event) => onFilterChange({ ...filters, accountId: event.target.value })}
            placeholder="ACC1001"
          />
        </label>
      </div>

      <div className="table-wrap">
        <table>
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
          <tbody>
            {visibleTransactions.map((txn) => (
              <tr
                key={txn.transactionId}
                className={`${newIdsSet.has(txn.transactionId) ? 'row-new' : ''} ${
                  Number.isFinite(pinnedId) && txn.transactionId === pinnedId ? 'row-pinned' : ''
                }`}
              >
                <td className="cell-data">{txn.transactionId}</td>
                <td>
                  <span className="source-label">
                    {txn.sourceType} {txn.sourceName}
                  </span>
                </td>
                <td className="cell-data">{txn.accountId}</td>
                <td>{txn.payeeName ?? txn.payeeId}</td>
                <td className="cell-amount">
                  {txn.currency} {txn.amount.toLocaleString()}
                </td>
                <td>{txn.transactionType}</td>
                <td>
                  <StatusIndicator status={txn.status} />
                </td>
                <td className="cell-data">{new Date(txn.timestamp).toLocaleString()}</td>
              </tr>
            ))}
            {visibleTransactions.length === 0 && (
              <tr>
                <td colSpan={8} className="empty-row">
                  No transactions found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
