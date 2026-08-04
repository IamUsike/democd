import { useMemo, useState } from 'react';
import type { Alert, AlertStatus } from '../types/alert';

type AlertDetailProps = {
  alert: Alert | null;
  updating: boolean;
  onChangeStatus: (alertId: number, status: AlertStatus, notes?: string) => Promise<void>;
};

function allowedTransitions(status: AlertStatus): AlertStatus[] {
  if (status === 'OPEN') return ['ACKNOWLEDGED'];
  if (status === 'ACKNOWLEDGED') return ['INVESTIGATING', 'DISMISSED'];
  if (status === 'INVESTIGATING') return ['CLOSED', 'DISMISSED'];
  return [];
}

export function AlertDetail({ alert, updating, onChangeStatus }: AlertDetailProps) {
  const [notes, setNotes] = useState('');
  const transitions = useMemo(
    () => (alert ? allowedTransitions(alert.status) : []),
    [alert],
  );

  if (!alert) {
    return (
      <section className="card">
        <header className="section-header">
          <h2>Alert Detail</h2>
        </header>
        <p className="muted">Select an alert to inspect lifecycle details.</p>
      </section>
    );
  }

  return (
    <section className="card">
      <header className="section-header">
        <h2>Alert Detail</h2>
        <span className={`severity ${alert.severity.toLowerCase()}`}>{alert.severity}</span>
      </header>

      <div className="detail-grid">
        <p>
          <strong>ID:</strong> {alert.alertId}
        </p>
        <p>
          <strong>Status:</strong> {alert.status}
        </p>
        <p>
          <strong>Rule:</strong> {alert.ruleTriggered}
        </p>
        <p>
          <strong>Created:</strong> {new Date(alert.createdAt).toLocaleString()}
        </p>
        <p>
          <strong>Source:</strong> {alert.sourceName ?? '-'}
        </p>
        <p>
          <strong>Account:</strong> {alert.accountId ?? '-'}
        </p>
        <p>
          <strong>Transaction:</strong> {alert.transactionId ?? '-'}
        </p>
      </div>

      <label className="notes-field">
        Resolution Notes
        <textarea
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          placeholder="Add investigation notes before closing or dismissing"
          rows={3}
        />
      </label>

      <div className="actions">
        {transitions.map((status) => (
          <button
            type="button"
            key={status}
            disabled={updating}
            onClick={() => onChangeStatus(alert.alertId, status, notes)}
          >
            Mark {status}
          </button>
        ))}
      </div>
    </section>
  );
}

