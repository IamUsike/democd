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

/**
 * Paginated alert list with virtual scrolling.
 * Only visible rows are mounted; parent `.virtual-list` must have a fixed height
 * so this div — not the page — is the scroll container.
 */
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
  const showSkeleton = loading && alerts.length === 0;
  const rowCount = showSkeleton ? 8 : alerts.length;

  // Absolute-positioned rows inside a tall inner spacer = O(visible) DOM nodes.
  const rowVirtualizer = useVirtualizer({
    count: rowCount,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 96,
    overscan: 8,
    // Measure real multi-rule row heights (Firefox falls back to estimateSize).
    measureElement:
      typeof window !== 'undefined' && !navigator.userAgent.includes('Firefox')
        ? (element) => element.getBoundingClientRect().height
        : undefined,
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
        {showSkeleton ? (
          <div
            className="virtual-list-inner"
            style={{ height: `${rowVirtualizer.getTotalSize()}px` }}
          >
            {rowVirtualizer.getVirtualItems().map((virtualRow) => (
              <div
                key={virtualRow.key}
                data-index={virtualRow.index}
                ref={rowVirtualizer.measureElement}
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
        ) : alerts.length > 0 ? (
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
                  data-index={virtualRow.index}
                  ref={rowVirtualizer.measureElement}
                  className={`alert-item${selectedAlertId === alert.alertId ? ' active' : ''}`}
                  onClick={() => onSelect(alert.alertId)}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    transform: `translateY(${virtualRow.start}px)`,
                  }}
                >
                  <div className="alert-item-main">
                    <div className="alert-item-header">
                      <strong className="alert-item-id">#{alert.alertId}</strong>
                      <StatusIndicator status={alert.status} />
                    </div>
                    {alert.ruleTypes && alert.ruleTypes.length > 1 ? (
                      <ul className="alert-item-rules">
                        {alert.ruleTypes.map((ruleType) => (
                          <li key={ruleType}>{ruleType.replace(/_/g, ' ')}</li>
                        ))}
                      </ul>
                    ) : (
                      <p className="alert-item-rule">{alert.ruleTriggered}</p>
                    )}
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
        ) : (
          !loading && <p className="muted alerts-empty">No alerts match filters.</p>
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
