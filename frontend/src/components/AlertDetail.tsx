import { useEffect, useMemo, useState } from 'react';
import type { Alert, AlertStatus } from '../types/alert';
import { StatusIndicator } from './StatusIndicator';

type AlertDetailProps = {
  alert: Alert | null;
  loading?: boolean;
  updating: boolean;
  onChangeStatus: (alertId: number, status: AlertStatus, notes?: string) => Promise<void>;
};

function allowedTransitions(status: AlertStatus): AlertStatus[] {
  if (status === 'OPEN') return ['ACKNOWLEDGED'];
  if (status === 'ACKNOWLEDGED') return ['INVESTIGATING', 'DISMISSED'];
  if (status === 'INVESTIGATING') return ['CLOSED', 'DISMISSED'];
  return [];
}

function TimelineRow({ label, ts }: { label: string; ts: string | null | undefined }) {
  if (!ts) return null;
  return (
    <li className="tl-row">
      <span className="tl-dot" />
      <span className="tl-label">{label}</span>
      <span className="tl-time muted">{new Date(ts).toLocaleString()}</span>
    </li>
  );
}

export function AlertDetail({ alert, loading = false, updating, onChangeStatus }: AlertDetailProps) {
  const [notes, setNotes] = useState('');
  const transitions = useMemo(
    () => (alert ? allowedTransitions(alert.status) : []),
    [alert],
  );

  useEffect(() => {
    setNotes('');
  }, [alert?.alertId]);

  if (loading && !alert) {
    return (
      <section className="card">
        <header className="section-header">
          <h2>Alert Detail</h2>
        </header>
        <div className="skeleton-row detail-skeleton" />
        <div className="skeleton-row detail-skeleton" />
        <div className="skeleton-row detail-skeleton" />
      </section>
    );
  }

  if (!alert) {
    return (
      <section className="card">
        <header className="section-header">
          <h2>Alert Detail</h2>
        </header>
        <p className="muted">Select an alert to view its details and lifecycle.</p>
      </section>
    );
  }

  const transactionIds = alert.transactionIds?.length
    ? alert.transactionIds
    : alert.transactionId != null
      ? [alert.transactionId]
      : [];

  return (
    <section className="card alert-detail">
      <header className="section-header">
        <h2>Alert #{alert.alertId}</h2>
        <span className={`severity ${alert.severity.toLowerCase()}`}>{alert.severity}</span>
      </header>

      {loading && <p className="muted">Refreshing detail…</p>}

      {alert.failingReason && (
        <div className="alert-failing-reason">
          <p className="alert-fr-label">Failing Reason</p>
          <p className="alert-fr-text">{alert.failingReason}</p>
        </div>
      )}

      <div className="detail-grid">
        <p>
          <strong>Status</strong> <StatusIndicator status={alert.status} />
        </p>
        <p>
          <strong>Rule</strong> <span>{alert.ruleTriggered}</span>
        </p>
        <p>
          <strong>Account</strong>{' '}
          <span className="cell-data">{alert.accountId ?? '—'}</span>
        </p>
        <p>
          <strong>Source</strong> <span>{alert.sourceName ?? '—'}</span>
        </p>
        <p>
          <strong>Transactions</strong>{' '}
          <span className="cell-data">
            {transactionIds.length > 0 ? transactionIds.join(', ') : '—'}
          </span>
        </p>
        <p>
          <strong>Opened</strong>{' '}
          <span className="cell-data">{new Date(alert.createdAt).toLocaleString()}</span>
        </p>
      </div>

      {alert.ruleDescription && (
        <div className="alert-rule-desc">
          <p className="alert-rd-label">Rule Description</p>
          <p className="alert-rd-text">{alert.ruleDescription}</p>
        </div>
      )}

      <div className="alert-timeline">
        <p className="alert-tl-label">Lifecycle</p>
        <ul className="tl-list">
          <TimelineRow label="OPEN" ts={alert.createdAt} />
          <TimelineRow label="ACKNOWLEDGED" ts={alert.acknowledgedAt} />
          <TimelineRow label="INVESTIGATING" ts={alert.investigatingAt} />
          <TimelineRow label="DISMISSED" ts={alert.dismissedAt} />
          <TimelineRow label="CLOSED" ts={alert.closedAt} />
        </ul>
      </div>

      {alert.resolutionNotes && (
        <div className="alert-rule-desc">
          <p className="alert-rd-label">Resolution Notes</p>
          <p className="alert-rd-text">{alert.resolutionNotes}</p>
        </div>
      )}

      {transitions.length > 0 && (
        <>
          <label className="notes-field">
            Notes
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add investigation notes before closing or dismissing…"
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
        </>
      )}
    </section>
  );
}
