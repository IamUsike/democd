import { useState } from 'react';
import { AlertsPanel } from '../components/AlertsPanel';
import { AlertDetail } from '../components/AlertDetail';
import { useAlerts } from '../hooks/useAlerts';
import type { AlertFilters, AlertStatus } from '../types/alert';

export function AlertsPage() {
  const {
    alerts,
    selectedAlert,
    selectedAlertId,
    loading,
    detailLoading,
    warning,
    updating,
    filters,
    totalCount,
    hasNext,
    setFilters,
    setPage,
    selectAlert,
    changeStatus,
  } = useAlerts();

  const [searchDraft, setSearchDraft] = useState(filters.q ?? '');

  async function onChangeStatus(alertId: number, status: AlertStatus, notes?: string) {
    await changeStatus(alertId, status, notes);
  }

  function onFilterChange(patch: Partial<AlertFilters>) {
    setFilters((prev) => ({
      ...prev,
      ...patch,
      page: patch.page !== undefined ? patch.page : 0,
    }));
  }

  return (
    <main className="page-frame">
      <header className="page-header">
        <h2>Alerts</h2>
        <p>Filter, inspect and manage alert lifecycle transitions</p>
      </header>

      {warning && (
        <p className="state-message" role="status">
          {warning}
        </p>
      )}

      <div className="filters alerts-filters">
        <label>
          Status
          <select
            value={filters.status ?? ''}
            onChange={(event) =>
              onFilterChange({ status: event.target.value as AlertFilters['status'] })
            }
          >
            <option value="">Active (default)</option>
            <option value="ALL">All history</option>
            <option value="OPEN">OPEN</option>
            <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
            <option value="INVESTIGATING">INVESTIGATING</option>
            <option value="CLOSED">CLOSED</option>
            <option value="DISMISSED">DISMISSED</option>
          </select>
        </label>

        <label>
          Severity
          <select
            value={filters.severity ?? ''}
            onChange={(event) =>
              onFilterChange({ severity: event.target.value as AlertFilters['severity'] })
            }
          >
            <option value="">All</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>
        </label>

        <label>
          Source Type
          <select
            value={filters.sourceType ?? ''}
            onChange={(event) =>
              onFilterChange({ sourceType: event.target.value as AlertFilters['sourceType'] })
            }
          >
            <option value="">All</option>
            <option value="BANK">BANK</option>
            <option value="MERCHANT">MERCHANT</option>
          </select>
        </label>

        <label>
          Source ID
          <input
            value={filters.sourceId ?? ''}
            onChange={(event) => onFilterChange({ sourceId: event.target.value })}
            placeholder="HSBC-UK"
          />
        </label>

        <label>
          Account ID
          <input
            value={filters.accountId ?? ''}
            onChange={(event) => onFilterChange({ accountId: event.target.value })}
            placeholder="ACC1001"
          />
        </label>

        <label>
          Sort
          <select
            value={filters.sort ?? 'createdAt,desc'}
            onChange={(event) => onFilterChange({ sort: event.target.value })}
          >
            <option value="createdAt,desc">Newest first</option>
            <option value="createdAt,asc">Oldest first</option>
            <option value="severity,desc">Severity</option>
            <option value="status,asc">Status</option>
          </select>
        </label>

        <label>
          Search
          <input
            value={searchDraft}
            onChange={(event) => {
              const value = event.target.value;
              setSearchDraft(value);
              onFilterChange({ q: value });
            }}
            placeholder="Account / source / rule"
          />
        </label>
      </div>

      <div className="grid-two">
        <AlertsPanel
          alerts={alerts}
          selectedAlertId={selectedAlertId}
          loading={loading}
          totalCount={totalCount}
          page={filters.page ?? 0}
          hasNext={hasNext}
          onSelect={selectAlert}
          onPageChange={setPage}
        />
        <AlertDetail
          alert={selectedAlert}
          loading={detailLoading}
          updating={updating}
          onChangeStatus={onChangeStatus}
        />
      </div>
    </main>
  );
}
