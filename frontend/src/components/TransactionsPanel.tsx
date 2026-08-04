import type { Transaction, TransactionFilters } from '../types/transaction';

type TransactionsPanelProps = {
  transactions: Transaction[];
  filters: TransactionFilters;
  loading: boolean;
  onFilterChange: (nextFilters: TransactionFilters) => void;
};

export function TransactionsPanel({
  transactions,
  filters,
  loading,
  onFilterChange,
}: TransactionsPanelProps) {
  return (
    <section className="card">
      <header className="section-header">
        <h2>Transactions</h2>
        <span className="muted">{loading ? 'Loading...' : `${transactions.length} results`}</span>
      </header>

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
            {transactions.map((txn) => (
              <tr key={txn.transactionId}>
                <td>{txn.transactionId}</td>
                <td>
                  <span className="badge">{txn.sourceType}</span> {txn.sourceName}
                </td>
                <td>{txn.accountId}</td>
                <td>{txn.payeeName ?? txn.payeeId}</td>
                <td>
                  {txn.currency} {txn.amount.toLocaleString()}
                </td>
                <td>{txn.transactionType}</td>
                <td>{txn.status}</td>
                <td>{new Date(txn.timestamp).toLocaleString()}</td>
              </tr>
            ))}
            {transactions.length === 0 && (
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

