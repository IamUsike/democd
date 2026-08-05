import { useState } from 'react';
import { TransactionsPanel } from '../components/TransactionsPanel';
import { useTransactions } from '../hooks/useTransactions';
import type { TransactionFilters } from '../types/transaction';

export function TransactionsPage() {
  const [filters, setFilters] = useState<TransactionFilters>({
    sourceType: '',
    sourceId: '',
    accountId: '',
  });

  const [quickSearch, setQuickSearch] = useState('');
  const [pinnedTransactionId, setPinnedTransactionId] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [paused, setPaused] = useState(false);

  const {
    transactions,
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
    <main className="page-frame">
      <header className="page-header">
        <h2>Transactions</h2>
        <p>Filter by source and account</p>
      </header>

      {warning && (
        <p className="state-message" role="status">
          {warning}
        </p>
      )}

      <TransactionsPanel
        transactions={transactions}
        filters={filters}
        quickSearch={quickSearch}
        pinnedTransactionId={pinnedTransactionId}
        autoRefresh={autoRefresh}
        paused={paused}
        lastUpdatedAt={lastUpdatedAt}
        newTransactionIds={newTransactionIds}
        loading={loading}
        onFilterChange={setFilters}
        onQuickSearchChange={setQuickSearch}
        onPinnedTransactionIdChange={setPinnedTransactionId}
        onAutoRefreshChange={setAutoRefresh}
        onPausedChange={setPaused}
        onRefreshNow={refreshNow}
      />
    </main>
  );
}
