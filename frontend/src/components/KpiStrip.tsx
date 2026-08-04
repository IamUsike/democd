import type { DashboardSummary } from '../types/dashboard';

type KpiStripProps = {
  summary: DashboardSummary;
  loading: boolean;
};

export function KpiStrip({ summary, loading }: KpiStripProps) {
  const cards = [
    { label: 'Total Transactions', value: summary.totalTransactions },
    { label: 'Total Alerts', value: summary.totalAlerts },
    { label: 'Open Alerts', value: summary.openAlerts },
    { label: 'Closed Alerts', value: summary.closedAlerts },
    { label: 'High Severity Alerts', value: summary.highSeverityAlerts },
  ];

  return (
    <section className="card">
      <header className="section-header">
        <h2>Dashboard KPIs</h2>
        <span className="muted">{loading ? 'Loading...' : 'Live summary'}</span>
      </header>
      <div className="kpi-grid">
        {cards.map((card) => (
          <article key={card.label} className="kpi-card">
            <p>{card.label}</p>
            <strong>{card.value}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}

