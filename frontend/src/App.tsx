import { useMemo, useState } from 'react';
import './App.css';
import { API_BASE_URL } from './api/http';
import { AlertDetail } from './components/AlertDetail';
import { AlertsPanel } from './components/AlertsPanel';
import { KpiStrip } from './components/KpiStrip';
import { TransactionsPanel } from './components/TransactionsPanel';
import { useAlerts } from './hooks/useAlerts';
import { useDashboardKpis } from './hooks/useDashboardKpis';
import { useTransactions } from './hooks/useTransactions';
import type { AlertStatus } from './types/alert';
import type { TransactionFilters } from './types/transaction';

function App() {
  const [filters, setFilters] = useState<TransactionFilters>({
    sourceType: '',
    sourceId: '',
    accountId: '',
  });

  const { summary, loading: kpiLoading, warning: kpiWarning } = useDashboardKpis();
  const { transactions, loading: transactionsLoading, warning: transactionsWarning } =
    useTransactions(filters);
  const {
    alerts,
    selectedAlert,
    selectedAlertId,
    loading: alertsLoading,
    warning: alertsWarning,
    updating,
    selectAlert,
    changeStatus,
  } = useAlerts();

  const allWarnings = useMemo(
    () => [kpiWarning, transactionsWarning, alertsWarning].filter(Boolean) as string[],
    [kpiWarning, transactionsWarning, alertsWarning],
  );

  async function onChangeStatus(alertId: number, status: AlertStatus, notes?: string) {
    await changeStatus(alertId, status, notes);
  }

  return (
    <main className="dashboard-page">
      <section className="hero">
        <div className="hero-content">
          <p className="hero-tag">Transaction Monitoring Platform</p>
          <h1>Protect payment flows in real time with one operator dashboard.</h1>
          <p>
            Ingest BANK and MERCHANT transactions, detect suspicious activity,
            and move alerts through OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED or DISMISSED.
          </p>
          <p className="api-hint">API base: {API_BASE_URL}</p>
        </div>
        <div className="hero-card" aria-hidden="true">
          <h3>Live Monitoring</h3>
          <p>MTTD path: same request detection (sync rule evaluation)</p>
          <div className="hero-metrics">
            <span>Sources: BANK + MERCHANT</span>
            <span>Lifecycle tracking enabled</span>
          </div>
        </div>
      </section>

      {allWarnings.length > 0 && (
        <section className="warning-strip" role="status">
          {allWarnings.map((warning) => (
            <p key={warning}>{warning}</p>
          ))}
        </section>
      )}

      <KpiStrip summary={summary} loading={kpiLoading} />

      <div className="grid-two">
        <TransactionsPanel
          transactions={transactions}
          filters={filters}
          loading={transactionsLoading}
          onFilterChange={setFilters}
        />
        <AlertsPanel
          alerts={alerts}
          selectedAlertId={selectedAlertId}
          loading={alertsLoading}
          onSelect={selectAlert}
        />
      </div>

      <AlertDetail alert={selectedAlert} updating={updating} onChangeStatus={onChangeStatus} />
    </main>
  );
}

export default App;
