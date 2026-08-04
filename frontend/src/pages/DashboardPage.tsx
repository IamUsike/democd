import { API_BASE_URL } from '../api/http';
import { KpiStrip } from '../components/KpiStrip';
import { useDashboardKpis } from '../hooks/useDashboardKpis';

export function DashboardPage() {
  const { summary, loading, warning } = useDashboardKpis();

  return (
    <main className="page-frame">
      <section className="hero">
        <div className="hero-content">
          <p className="hero-tag">Transaction Monitoring Platform</p>
          <h2>Protect payment flows in real time from one clean operator view.</h2>
          <p>
            Track volume, alert pressure, and high-severity risk at a glance.
            Switch to Transactions and Alerts for deeper investigation.
          </p>
          <p className="api-hint">API base: {API_BASE_URL}</p>
        </div>
        <div className="hero-card" aria-hidden="true">
          <h3>Live Monitoring</h3>
          <p>MTTD path: same-request detection (MVP sync flow)</p>
          <div className="hero-metrics">
            <span>Sources: BANK + MERCHANT</span>
            <span>Lifecycle tracking: OPEN to CLOSED / DISMISSED</span>
          </div>
        </div>
      </section>

      {warning && (
        <section className="warning-strip" role="status">
          <p>{warning}</p>
        </section>
      )}

      <KpiStrip summary={summary} loading={loading} />
    </main>
  );
}

