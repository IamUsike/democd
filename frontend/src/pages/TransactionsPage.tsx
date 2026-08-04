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

  const { transactions, loading, warning } = useTransactions(filters);

  return (
    <main className="page-frame">
      <header className="page-header">
        <h2>Transactions</h2>
        <p>Filter by source and account to inspect incoming payment activity.</p>
      </header>

      {warning && (
        <section className="warning-strip" role="status">
          <p>{warning}</p>
        </section>
      )}

      <TransactionsPanel
        transactions={transactions}
        filters={filters}
        loading={loading}
        onFilterChange={setFilters}
      />
    </main>
  );
}

