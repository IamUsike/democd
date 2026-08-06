import type { Alert } from '../types/alert';
import type { StatusFilter } from '../hooks/useAlerts';
import { ALL_STATUSES } from '../hooks/useAlerts';
import { StatusIndicator } from './StatusIndicator';

const STATUS_LABELS: Record<string, string> = {
  ALL: 'All',
  OPEN: 'Open',
  ACKNOWLEDGED: 'Acknowledged',
  INVESTIGATING: 'Investigating',
  CLOSED: 'Closed',
  DISMISSED: 'Dismissed',
};

type AlertsPanelProps = {
  alerts: Alert[];
  selectedAlertId: number | null;
  loading: boolean;
  statusFilter: StatusFilter;
  onStatusFilter: (status: StatusFilter) => void;
  onSelect: (alertId: number) => void;
};

export function AlertsPanel({
  alerts,
  selectedAlertId,
  loading,
  statusFilter,
  onStatusFilter,
  onSelect,
}: AlertsPanelProps) {
  return (
    <section className="card alerts-panel">
      <header className="section-header">
        <h2>Alerts</h2>
        <span className="muted">
          {loading ? 'Loading…' : `${alerts.length} result${alerts.length !== 1 ? 's' : ''}`}
        </span>
      </header>

      {/* ── Status filter tabs ── */}
      <div className="status-tabs" role="tablist" aria-label="Filter alerts by status">
        {ALL_STATUSES.map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={statusFilter === tab}
            className={`status-tab${statusFilter === tab ? ' active' : ''}`}
            onClick={() => onStatusFilter(tab)}
          >
            {STATUS_LABELS[tab]}
          </button>
        ))}
      </div>

      {/* ── Alert list ── */}
      <div className="alerts-list">
        {alerts.map((alert) => (
          <button
            key={alert.alertId}
            type="button"
            className={`alert-item${selectedAlertId === alert.alertId ? ' active' : ''}`}
            onClick={() => onSelect(alert.alertId)}
          >
            <div className="alert-item-main">
              <div className="alert-item-header">
                <strong className="alert-item-id">#{alert.alertId}</strong>
                <StatusIndicator status={alert.status} />
              </div>
              <p className="alert-item-rule">{alert.ruleTriggered}</p>
              {alert.failingReason && (
                <p className="alert-item-reason">{alert.failingReason}</p>
              )}
            </div>
            <div className="alert-item-meta">
              <span className={`severity ${alert.severity.toLowerCase()}`}>
                {alert.severity}
              </span>
              <span className="alert-item-time muted">
                {new Date(alert.createdAt).toLocaleDateString()}
              </span>
            </div>
          </button>
        ))}
        {!loading && alerts.length === 0 && (
          <p className="muted alerts-empty">No alerts match this filter.</p>
        )}
      </div>
    </section>
  );
}
