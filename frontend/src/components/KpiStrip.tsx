import { useNavigate } from 'react-router-dom';
import type { DashboardSummary } from '../types/dashboard';

type KpiStripProps = {
  summary: DashboardSummary;
  loading: boolean;
};

export function KpiStrip({ summary, loading }: KpiStripProps) {
  const navigate = useNavigate();

  const cards = [
    { label: 'Total Transactions', value: summary.totalTransactions, path: '/transactions' },
    { label: 'Total Alerts', value: summary.totalAlerts, path: '/alerts', filter: 'ALL' },
    { label: 'Open Alerts', value: summary.openAlerts, path: '/alerts', filter: 'OPEN' },
    { label: 'Closed Alerts', value: summary.closedAlerts, path: '/alerts', filter: 'CLOSED' },
    { label: 'High Severity Alerts', value: summary.highSeverityAlerts, path: '/alerts', severity: 'HIGH' },
  ];

  const handleCardClick = (card: typeof cards[number]) => {
    if (!card.path) return;
    const params = new URLSearchParams();
    if (card.filter) params.set('status', card.filter);
    if (card.severity) params.set('severity', card.severity);
    const searchString = params.toString();
    navigate(`${card.path}${searchString ? `?${searchString}` : ''}`);
  };

  return (
    <section className="card">
      <header className="section-header">
        <h2>Dashboard KPIs</h2>
        <span className="muted">{loading ? 'Loading...' : 'Click any box to inspect filtered list'}</span>
      </header>
      <div className="kpi-grid">
        {cards.map((card) => (
          <article
            key={card.label}
            className="kpi-card kpi-card-interactive"
            onClick={() => handleCardClick(card)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleCardClick(card);
              }
            }}
            title={`Click to view ${card.label}`}
          >
            <div className="kpi-card-header">
              <p>{card.label}</p>
              <span className="kpi-link-icon">↗</span>
            </div>
            <strong>{card.value}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}

