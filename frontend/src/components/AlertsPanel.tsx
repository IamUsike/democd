import type { Alert } from '../types/alert';
import { StatusIndicator } from './StatusIndicator';

type AlertsPanelProps = {
  alerts: Alert[];
  selectedAlertId: number | null;
  loading: boolean;
  onSelect: (alertId: number) => void;
};

export function AlertsPanel({ alerts, selectedAlertId, loading, onSelect }: AlertsPanelProps) {
  const activeAlerts = alerts.filter(
    (alert) => alert.status !== 'CLOSED' && alert.status !== 'DISMISSED',
  );

  return (
    <section className="card">
      <header className="section-header">
        <h2>Active Alerts</h2>
        <span className="muted">{loading ? 'Loading...' : `${activeAlerts.length} active`}</span>
      </header>
      <div className="alerts-list">
        {activeAlerts.map((alert) => (
          <button
            key={alert.alertId}
            type="button"
            className={`alert-item ${selectedAlertId === alert.alertId ? 'active' : ''}`}
            onClick={() => onSelect(alert.alertId)}
          >
            <div>
              <strong>Alert #{alert.alertId}</strong>
              <p>{alert.ruleTriggered}</p>
            </div>
            <div className="alert-meta">
              <span className="severity">{alert.severity}</span>
              <StatusIndicator status={alert.status} />
            </div>
          </button>
        ))}
        {activeAlerts.length === 0 && <p className="muted">No active alerts.</p>}
      </div>
    </section>
  );
}
