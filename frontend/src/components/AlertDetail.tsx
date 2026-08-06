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

  const ruleTypes = alert.ruleTypes?.length
    ? alert.ruleTypes
    : alert.ruleType
      ? alert.ruleType.split(',').map((part) => part.trim()).filter(Boolean)
      : [];
  const failingReasons = alert.failingReason
    ? alert.failingReason.split(' | ').map((part) => part.trim()).filter(Boolean)
    : [];
  const ruleDescriptions = alert.ruleDescription
    ? alert.ruleDescription.split(' | ').map((part) => part.trim()).filter(Boolean)
    : [];

  return (
    <section className="card alert-detail">
      <header className="section-header">
        <h2>Alert #{alert.alertId}</h2>
        <span className={`severity ${alert.severity.toLowerCase()}`}>{alert.severity}</span>
      </header>

      {loading && <p className="muted">Refreshing detail…</p>}

      {failingReasons.length > 0 && (
        <div className="alert-failing-reason">
          <p className="alert-fr-label">
            {failingReasons.length > 1 ? 'Failing Reasons' : 'Failing Reason'}
          </p>
          {failingReasons.length > 1 ? (
            <ul className="alert-multi-list">
              {failingReasons.map((reason, index) => (
                <li key={`${reason}-${index}`}>
                  {ruleTypes[index] ? (
                    <strong>{ruleTypes[index].replace(/_/g, ' ')}: </strong>
                  ) : null}
                  {reason}
                </li>
              ))}
            </ul>
          ) : (
            <p className="alert-fr-text">{failingReasons[0]}</p>
          )}
        </div>
      )}

      <div className="detail-grid">
        <p>
          <strong>Status</strong> <StatusIndicator status={alert.status} />
        </p>
        <div className="detail-field">
          <strong>{ruleTypes.length > 1 ? 'Rules' : 'Rule'}</strong>{' '}
          {ruleTypes.length > 1 ? (
            <ul className="alert-multi-list alert-multi-list-inline">
              {ruleTypes.map((ruleType) => (
                <li key={ruleType}>{ruleType.replace(/_/g, ' ')}</li>
              ))}
            </ul>
          ) : (
            <span>{alert.ruleTriggered}</span>
          )}
        </div>
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

      {ruleDescriptions.length > 0 && (
        <div className="alert-rule-desc">
          <p className="alert-rd-label">
            {ruleDescriptions.length > 1 ? 'Rule Descriptions' : 'Rule Description'}
          </p>
          {ruleDescriptions.length > 1 ? (
            <ul className="alert-multi-list">
              {ruleDescriptions.map((description, index) => (
                <li key={`${description}-${index}`}>
                  {ruleTypes[index] ? (
                    <strong>{ruleTypes[index].replace(/_/g, ' ')}: </strong>
                  ) : null}
                  {description}
                </li>
              ))}
            </ul>
          ) : (
            <p className="alert-rd-text">{ruleDescriptions[0]}</p>
          )}
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
