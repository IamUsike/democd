import { useState } from 'react';
import { TransactionsPanel } from '../components/TransactionsPanel';
import { useTransactions } from '../hooks/useTransactions';
import type { TransactionFilters } from '../types/transaction';

export function TransactionsPage() {
  const [filters, setFilters] = useState<TransactionFilters>({
    sourceType: '',
    sourceId: '',
    accountId: '',
    q: '',
    sort: 'timestamp,desc',
    page: 0,
    size: 50,
  });

  const [pinnedTransactionId, setPinnedTransactionId] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [paused, setPaused] = useState(false);

  const {
    transactions,
    totalCount,
    hasNext,
    loading,
    warning,
    lastUpdatedAt,
    newTransactionIds,
    refreshNow,
  } = useTransactions(filters, {
    autoRefresh,
    paused,
    refreshIntervalMs: 3000,
  });

  return (
    <main className="page-frame transactions-page">
      <header className="page-header">
        <h2>Transactions</h2>
        <p>Paginated list with delta poll for new rows</p>
      </header>

      {warning && (
        <p className="state-message" role="status">
          {warning}
        </p>
      )}

      <TransactionsPanel
        transactions={transactions}
        filters={filters}
        pinnedTransactionId={pinnedTransactionId}
        autoRefresh={autoRefresh}
        paused={paused}
        lastUpdatedAt={lastUpdatedAt}
        newTransactionIds={newTransactionIds}
        loading={loading}
        totalCount={totalCount}
        hasNext={hasNext}
        onFilterChange={setFilters}
        onPinnedTransactionIdChange={setPinnedTransactionId}
        onAutoRefreshChange={setAutoRefresh}
        onPausedChange={setPaused}
        onRefreshNow={refreshNow}
        onPageChange={(page) => setFilters((prev) => ({ ...prev, page }))}
      />
    </main>
  );
}
