import type { DashboardAnalytics, GraphPoint } from '../types/dashboard';

type DashboardGraphsProps = {
  analytics: DashboardAnalytics | null;
  loading: boolean;
  lastCalculatedAt: Date | null;
  source: 'LIVE' | 'DEMO' | null;
  infoMessage?: string | null;
  onCalculate: () => void;
  onShowDemo: () => void;
};

const COLOR_PALETTE: Record<string, string> = {
  // Severity Level Colors
  HIGH: '#ef4444',
  MEDIUM: '#f59e0b',
  LOW: '#10b981',

  // Alert Lifecycle Status Colors
  OPEN: '#f59e0b',
  ACKNOWLEDGED: '#3b82f6',
  INVESTIGATING: '#8b5cf6',
  CLOSED: '#10b981',
  DISMISSED: '#64748b',

  // Transaction Status Colors
  COMPLETED: '#10b981',
  SUCCESS: '#10b981',
  PENDING: '#f59e0b',
  FAILED: '#ef4444',
  REJECTED: '#ef4444',

  // Transaction Types / Sources
  PAYMENT: '#3b82f6',
  TRANSFER: '#6366f1',
  REFUND: '#06b6d4',
  DEBIT: '#8b5cf6',
  CREDIT: '#10b981',
  BANK: '#3b82f6',
  MERCHANT: '#ec4899',
};

const FALLBACK_COLORS = ['#3b82f6', '#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

function getPointColor(label: string, index: number): string {
  const upperKey = label.trim().toUpperCase();
  return COLOR_PALETTE[upperKey] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}

/** Largest-remainder percentages so legend values always sum to 100 when total > 0. */
function percentageShares(values: number[]): number[] {
  const total = values.reduce((sum, value) => sum + value, 0);
  if (total <= 0) {
    return values.map(() => 0);
  }

  const exact = values.map((value) => (value / total) * 100);
  const floors = exact.map((value) => Math.floor(value));
  let remainder = 100 - floors.reduce((sum, value) => sum + value, 0);
  const order = exact
    .map((value, index) => ({ index, frac: value - floors[index] }))
    .sort((a, b) => b.frac - a.frac);

  const result = [...floors];
  for (let i = 0; i < remainder; i += 1) {
    result[order[i % order.length].index] += 1;
  }
  return result;
}

function GraphBlock({ title, points }: { title: string; points: GraphPoint[] }) {
  const validPoints = points.filter((point) => point.value > 0);
  const total = validPoints.reduce((sum, point) => sum + point.value, 0);
  const percentages = percentageShares(validPoints.map((point) => point.value));
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
                    stroke={getPointColor(point.label, index)}
                    strokeDasharray={`${sliceSize} ${circumference - sliceSize}`}
                    strokeDashoffset={-sliceOffset}
                    strokeLinecap="butt"
                  />
                );
              })}
            </g>
          </svg>

          <div className="analytics-legend">
            {validPoints.map((point, index) => (
              <div key={point.label} className="analytics-legend-row">
                <span
                  className="analytics-legend-dot"
                  style={{ backgroundColor: getPointColor(point.label, index) }}
                  aria-hidden="true"
                />
                <span className="analytics-legend-label">{point.label}</span>
                <span className="analytics-legend-value">{point.value}</span>
                <span className="analytics-legend-pct">{percentages[index]}%</span>
              </div>
            ))}
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
  infoMessage,
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
          {source && (
            <span className={`analytics-source-badge ${source === 'DEMO' ? 'demo' : 'live'}`}>
              {source}
            </span>
          )}
          <span className="muted">
            {lastCalculatedAt
              ? `Last calculated at ${lastCalculatedAt.toLocaleTimeString()}`
              : 'Not calculated yet'}
          </span>
        </div>
      </header>

      {infoMessage && (
        <p className="state-message state-message--info" role="status">
          {infoMessage}
        </p>
      )}

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
