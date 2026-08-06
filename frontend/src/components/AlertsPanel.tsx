import { useRef } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import type { Alert } from '../types/alert';
import { StatusIndicator } from './StatusIndicator';

type AlertsPanelProps = {
  alerts: Alert[];
  selectedAlertId: number | null;
  loading: boolean;
  totalCount: number;
  page: number;
  hasNext: boolean;
  onSelect: (alertId: number) => void;
  onPageChange: (page: number) => void;
};

export function AlertsPanel({
  alerts,
  selectedAlertId,
  loading,
  totalCount,
  page,
  hasNext,
  onSelect,
  onPageChange,
}: AlertsPanelProps) {
  const parentRef = useRef<HTMLDivElement>(null);
  const rowVirtualizer = useVirtualizer({
    count: loading && alerts.length === 0 ? 8 : alerts.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 88,
    overscan: 6,
  });

  return (
    <section className="card alerts-panel">
      <header className="section-header">
        <h2>Alerts</h2>
        <span className="muted">
          {loading ? 'Loading…' : `${totalCount.toLocaleString()} total`}
        </span>
      </header>

      <div className="alerts-list virtual-list" ref={parentRef}>
        {loading && alerts.length === 0 ? (
          <div
            className="virtual-list-inner"
            style={{ height: `${rowVirtualizer.getTotalSize()}px` }}
          >
            {rowVirtualizer.getVirtualItems().map((virtualRow) => (
              <div
                key={virtualRow.key}
                className="skeleton-row alert-skeleton"
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: `${virtualRow.size}px`,
                  transform: `translateY(${virtualRow.start}px)`,
                }}
              />
            ))}
          </div>
        ) : (
          <div
            className="virtual-list-inner"
            style={{ height: `${rowVirtualizer.getTotalSize()}px` }}
          >
            {rowVirtualizer.getVirtualItems().map((virtualRow) => {
              const alert = alerts[virtualRow.index];
              if (!alert) return null;
              return (
                <button
                  key={alert.alertId}
                  type="button"
                  className={`alert-item${selectedAlertId === alert.alertId ? ' active' : ''}`}
                  onClick={() => onSelect(alert.alertId)}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: `${virtualRow.size}px`,
                    transform: `translateY(${virtualRow.start}px)`,
                  }}
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
              );
            })}
          </div>
        )}
        {!loading && alerts.length === 0 && (
          <p className="muted alerts-empty">No alerts match filters.</p>
        )}
      </div>

      <div className="pager">
        <button
          type="button"
          className="feed-button"
          disabled={page <= 0 || loading}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <span className="muted">Page {page + 1}</span>
        <button
          type="button"
          className="feed-button"
          disabled={!hasNext || loading}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}
