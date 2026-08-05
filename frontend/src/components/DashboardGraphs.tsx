import type { DashboardAnalytics, GraphPoint } from '../types/dashboard';

type DashboardGraphsProps = {
  analytics: DashboardAnalytics | null;
  loading: boolean;
  lastCalculatedAt: Date | null;
  source: 'LIVE' | 'DEMO';
  onCalculate: () => void;
  onShowDemo: () => void;
};

const PIE_COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

function GraphBlock({ title, points }: { title: string; points: GraphPoint[] }) {
  const validPoints = points.filter((point) => point.value > 0);
  const total = validPoints.reduce((sum, point) => sum + point.value, 0);
  const radius = 52;
  const size = 120;
  const circumference = 2 * Math.PI * radius;

  let cumulative = 0;

  return (
    <article className="analytics-graph-block">
      <h3>{title}</h3>
      {total === 0 ? (
        <p className="muted">No data available.</p>
      ) : (
        <div className="analytics-pie-layout">
          <svg className="analytics-pie" viewBox={`0 0 ${size} ${size}`} role="img" aria-label={title}>
            <circle
              className="analytics-pie-base"
              cx={size / 2}
              cy={size / 2}
              r={radius}
              strokeWidth="14"
              fill="none"
            />
            <g transform={`rotate(-90 ${size / 2} ${size / 2})`}>
              {validPoints.map((point, index) => {
                const sliceSize = (point.value / total) * circumference;
                const sliceOffset = cumulative;
                cumulative += sliceSize;

                return (
                  <circle
                    key={point.label}
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    strokeWidth="14"
                    fill="none"
                    stroke={PIE_COLORS[index % PIE_COLORS.length]}
                    strokeDasharray={`${sliceSize} ${circumference - sliceSize}`}
                    strokeDashoffset={-sliceOffset}
                    strokeLinecap="butt"
                  />
                );
              })}
            </g>
          </svg>

          <div className="analytics-legend">
            {validPoints.map((point, index) => {
              const pct = Math.round((point.value / total) * 100);
              return (
                <div key={point.label} className="analytics-legend-row">
                  <span
                    className="analytics-legend-dot"
                    style={{ backgroundColor: PIE_COLORS[index % PIE_COLORS.length] }}
                    aria-hidden="true"
                  />
                  <span className="analytics-legend-label">{point.label}</span>
                  <span className="analytics-legend-value">{point.value}</span>
                  <span className="analytics-legend-pct">{pct}%</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </article>
  );
}

export function DashboardGraphs({
  analytics,
  loading,
  lastCalculatedAt,
  source,
  onCalculate,
  onShowDemo,
}: DashboardGraphsProps) {
  return (
    <section className="card analytics-card">
      <header className="section-header analytics-header">
        <h2>Analytical Overview</h2>
        <div className="analytics-actions">
          <button type="button" className="feed-button" onClick={onCalculate} disabled={loading}>
            {loading ? 'Calculating...' : 'Calculate Graphs'}
          </button>
          <button type="button" className="feed-button" onClick={onShowDemo} disabled={loading}>
            Show Demo
          </button>
          <span className={`analytics-source-badge ${source === 'DEMO' ? 'demo' : 'live'}`}>
            {source}
          </span>
          <span className="muted">
            {lastCalculatedAt
              ? `Last calculated at ${lastCalculatedAt.toLocaleTimeString()}`
              : 'Not calculated yet'}
          </span>
        </div>
      </header>

      {!analytics && !loading && (
        <p className="state-message">Click "Calculate Graphs" for live data or "Show Demo" for a visual preview.</p>
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
