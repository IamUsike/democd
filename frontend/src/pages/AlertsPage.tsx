import { AlertsPanel } from '../components/AlertsPanel';
import { AlertDetail } from '../components/AlertDetail';
import { useAlerts } from '../hooks/useAlerts';
import type { AlertStatus } from '../types/alert';

export function AlertsPage() {
  const {
    alerts,
    selectedAlert,
    selectedAlertId,
    loading,
    warning,
    updating,
    selectAlert,
    changeStatus,
  } = useAlerts();

  async function onChangeStatus(alertId: number, status: AlertStatus, notes?: string) {
    await changeStatus(alertId, status, notes);
  }

  return (
    <main className="page-frame">
      <header className="page-header">
        <h2>Alerts</h2>
        <p>Review and update alert lifecycle</p>
      </header>

      {warning && (
        <p className="state-message" role="status">
          {warning}
        </p>
      )}

      <div className="grid-two">
        <AlertsPanel
          alerts={alerts}
          selectedAlertId={selectedAlertId}
          loading={loading}
          onSelect={selectAlert}
        />
        <AlertDetail alert={selectedAlert} updating={updating} onChangeStatus={onChangeStatus} />
      </div>
    </main>
  );
}
