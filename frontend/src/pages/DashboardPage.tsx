import { KpiStrip } from '../components/KpiStrip';
import { useDashboardKpis } from '../hooks/useDashboardKpis';

export function DashboardPage() {
  const { summary, loading, warning } = useDashboardKpis();

  return (
    <main className="page-frame">
      <header className="page-header">
        <h2>Dashboard</h2>
        <p>Transaction volume and alert counts</p>
      </header>

      {warning && (
        <p className="state-message" role="status">
          {warning}
        </p>
      )}

      <KpiStrip summary={summary} loading={loading} />
    </main>
  );
}
