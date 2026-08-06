import { useState } from 'react';
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

const TXN_TYPE_COLORS = [
  '#38bdf8', // Sky blue (PAYMENT)
  '#818cf8', // Indigo/Violet (TRANSFER)
  '#2dd4bf', // Teal (REFUND)
  '#c084fc', // Purple (DEBIT)
  '#fb7185', // Coral pink (CREDIT / OTHER)
  '#f472b6', // Pink
  '#34d399', // Emerald
];

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

  // Institution Sources
  BANK: '#3b82f6',
  MERCHANT: '#ec4899',
};

const FALLBACK_COLORS = ['#3b82f6', '#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

function getPointColor(title: string, label: string, index: number): string {
  if (title === 'Transactions by Type') {
    return TXN_TYPE_COLORS[index % TXN_TYPE_COLORS.length];
  }
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
  const [activeLabel, setActiveLabel] = useState<string | null>(null);
  const validPoints = points.filter((point) => point.value > 0);
  const total = validPoints.reduce((sum, point) => sum + point.value, 0);
  const percentages = percentageShares(validPoints.map((point) => point.value));
  const radius = 50;
  const size = 130;
  const circumference = 2 * Math.PI * radius;

  let cumulative = 0;
  const activePoint = validPoints.find((p) => p.label === activeLabel);
  const activeIndex = validPoints.findIndex((p) => p.label === activeLabel);

  return (
    <article className="analytics-graph-block">
      <div className="analytics-graph-header">
        <h3>{title}</h3>
        <span className="analytics-graph-count">{total} total</span>
      </div>
      {total === 0 ? (
        <p className="muted analytics-empty-state">No data available</p>
      ) : (
        <div className="analytics-pie-layout">
          <div className="analytics-donut-wrapper">
            <svg className="analytics-pie" viewBox={`0 0 ${size} ${size}`} role="img" aria-label={title}>
              <circle
                className="analytics-pie-base"
                cx={size / 2}
                cy={size / 2}
                r={radius}
                strokeWidth="12"
                fill="none"
              />
              <g transform={`rotate(-90 ${size / 2} ${size / 2})`}>
                {validPoints.map((point, index) => {
                  const sliceSize = (point.value / total) * circumference;
                  const sliceOffset = cumulative;
                  cumulative += sliceSize;
                  const isHovered = activeLabel === point.label;
                  const color = getPointColor(title, point.label, index);

                  return (
                    <circle
                      key={point.label}
                      cx={size / 2}
                      cy={size / 2}
                      r={radius}
                      strokeWidth={isHovered ? 17 : 13}
                      fill="none"
                      stroke={color}
                      strokeDasharray={`${sliceSize} ${circumference - sliceSize}`}
                      strokeDashoffset={-sliceOffset}
                      strokeLinecap="butt"
                      className={`analytics-pie-slice ${isHovered ? 'active' : ''}`}
                      onMouseEnter={() => setActiveLabel(point.label)}
                      onMouseLeave={() => setActiveLabel(null)}
                      style={{
                        transition: 'stroke-width 0.2s ease, opacity 0.2s ease',
                        opacity: activeLabel === null || isHovered ? 1 : 0.45,
                        cursor: 'pointer',
                      }}
                    />
                  );
                })}
              </g>
            </svg>
            <div className="analytics-donut-center">
              {activePoint ? (
                <>
                  <span className="donut-center-pct">{percentages[activeIndex]}%</span>
                  <span className="donut-center-sub" style={{ color: getPointColor(title, activePoint.label, activeIndex) }}>
                    {activePoint.label}
                  </span>
                </>
              ) : (
                <>
                  <span className="donut-center-val">{total}</span>
                  <span className="donut-center-sub">TOTAL</span>
                </>
              )}
            </div>
          </div>

          <div className="analytics-legend">
            {validPoints.map((point, index) => {
              const isHovered = activeLabel === point.label;
              const color = getPointColor(title, point.label, index);

              return (
                <div
                  key={point.label}
                  className={`analytics-legend-row ${isHovered ? 'active' : ''}`}
                  onMouseEnter={() => setActiveLabel(point.label)}
                  onMouseLeave={() => setActiveLabel(null)}
                  style={{
                    opacity: activeLabel === null || isHovered ? 1 : 0.4,
                    transition: 'opacity 0.2s ease, transform 0.15s ease',
                  }}
                >
                  <span
                    className="analytics-legend-dot"
                    style={{
                      backgroundColor: color,
                      boxShadow: isHovered ? `0 0 8px ${color}` : 'none',
                    }}
                    aria-hidden="true"
                  />
                  <span className="analytics-legend-label">{point.label}</span>
                  <span className="analytics-legend-value">{point.value}</span>
                  <span className="analytics-legend-pct" style={{ color: isHovered ? color : 'var(--text-secondary)' }}>
                    {percentages[index]}%
                  </span>
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
