import type { DashboardAnalytics, GraphPoint } from '../types/dashboard';

type DashboardGraphsProps = {
  analytics: DashboardAnalytics | null;
  loading: boolean;
  lastCalculatedAt: Date | null;
  onCalculate: () => void;
};

function GraphBlock({ title, points }: { title: string; points: GraphPoint[] }) {
  const maxValue = points.reduce((max, point) => Math.max(max, point.value), 0);

  return (
    <article className="analytics-graph-block">
      <h3>{title}</h3>
      {points.length === 0 && <p className="muted">No data available.</p>}
      <div className="analytics-bars">
        {points.map((point) => {
          const widthPercent = maxValue === 0 ? 0 : (point.value / maxValue) * 100;
          return (
            <div key={point.label} className="analytics-bar-row">
              <div className="analytics-bar-label">{point.label}</div>
              <div className="analytics-bar-track" aria-hidden="true">
                <div className="analytics-bar-fill" style={{ width: `${widthPercent}%` }} />
              </div>
              <div className="analytics-bar-value">{point.value}</div>
            </div>
          );
        })}
      </div>
    </article>
  );
}

export function DashboardGraphs({
  analytics,
  loading,
  lastCalculatedAt,
  onCalculate,
}: DashboardGraphsProps) {
  return (
    <section className="card analytics-card">
      <header className="section-header analytics-header">
        <h2>Analytical Overview</h2>
        <div className="analytics-actions">
          <button type="button" className="feed-button" onClick={onCalculate} disabled={loading}>
            {loading ? 'Calculating...' : 'Calculate Graphs'}
          </button>
          <span className="muted">
            {lastCalculatedAt
              ? `Last calculated at ${lastCalculatedAt.toLocaleTimeString()}`
              : 'Not calculated yet'}
          </span>
        </div>
      </header>

      {!analytics && !loading && (
        <p className="state-message">Click "Calculate Graphs" to build an overview from transactions and alerts.</p>
      )}

      {analytics && (
        <div className="analytics-grid">
          <GraphBlock title="Transactions by Type" points={analytics.transactionsByType} />
          <GraphBlock title="Transactions by Status" points={analytics.transactionsByStatus} />
          <GraphBlock title="Alerts by Status" points={analytics.alertsByStatus} />
          <GraphBlock title="Alerts by Severity" points={analytics.alertsBySeverity} />
        </div>
      )}
    </section>
  );
}

