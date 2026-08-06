import { useMemo, useState } from 'react';
import type { Alert, AlertStatus } from '../types/alert';
import { StatusIndicator } from './StatusIndicator';

type AlertDetailProps = {
  alert: Alert | null;
  updating: boolean;
  onChangeStatus: (alertId: number, status: AlertStatus, notes?: string) => Promise<void>;
};

function allowedTransitions(status: AlertStatus): AlertStatus[] {
  if (status === 'OPEN')          return ['ACKNOWLEDGED'];
  if (status === 'ACKNOWLEDGED')  return ['INVESTIGATING', 'DISMISSED'];
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
        <p className="muted">Select an alert to view its details and lifecycle.</p>
      </section>
    );
  }

  return (
    <section className="card alert-detail">
      {/* ── Header ── */}
      <header className="section-header">
        <h2>Alert #{alert.alertId}</h2>
        <span className={`severity ${alert.severity.toLowerCase()}`}>{alert.severity}</span>
      </header>

      {/* ── Failing Reason (prominent) ── */}
      {alert.failingReason && (
        <div className="alert-failing-reason">
          <p className="alert-fr-label">Failing Reason</p>
          <p className="alert-fr-text">{alert.failingReason}</p>
        </div>
      )}

      {/* ── Core fields ── */}
      <div className="detail-grid">
        <p><strong>Status</strong>   <StatusIndicator status={alert.status} /></p>
        <p><strong>Rule</strong>     <span>{alert.ruleTriggered}</span></p>
        <p><strong>Account</strong>  <span className="cell-data">{alert.accountId ?? '—'}</span></p>
        <p><strong>Source</strong>   <span>{alert.sourceName ?? '—'}</span></p>
        <p><strong>Transaction</strong> <span className="cell-data">{alert.transactionId ?? '—'}</span></p>
        <p><strong>Opened</strong>   <span className="cell-data">{new Date(alert.createdAt).toLocaleString()}</span></p>
      </div>

      {/* ── Rule description ── */}
      {alert.ruleDescription && (
        <div className="alert-rule-desc">
          <p className="alert-rd-label">Rule Description</p>
          <p className="alert-rd-text">{alert.ruleDescription}</p>
        </div>
      )}

      {/* ── Lifecycle timeline ── */}
      <div className="alert-timeline">
        <p className="alert-tl-label">Lifecycle</p>
        <ul className="tl-list">
          <TimelineRow label="OPEN"          ts={alert.createdAt} />
          <TimelineRow label="ACKNOWLEDGED"  ts={alert.acknowledgedAt} />
          <TimelineRow label="INVESTIGATING" ts={alert.investigatingAt} />
          <TimelineRow label="DISMISSED"     ts={alert.dismissedAt} />
          <TimelineRow label="CLOSED"        ts={alert.closedAt} />
        </ul>
      </div>

      {/* ── Resolution notes (if set) ── */}
      {alert.resolutionNotes && (
        <div className="alert-rule-desc">
          <p className="alert-rd-label">Resolution Notes</p>
          <p className="alert-rd-text">{alert.resolutionNotes}</p>
        </div>
      )}

      {/* ── Lifecycle actions ── */}
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
