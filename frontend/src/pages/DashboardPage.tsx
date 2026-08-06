import { DashboardGraphs } from '../components/DashboardGraphs';
import { KpiStrip } from '../components/KpiStrip';
import { useDashboardAnalytics } from '../hooks/useDashboardAnalytics';
import { useDashboardKpis } from '../hooks/useDashboardKpis';

export function DashboardPage() {
  const { summary, loading, warning } = useDashboardKpis();
  const {
    analytics,
    loading: analyticsLoading,
    warning: analyticsWarning,
    infoMessage: analyticsInfo,
    lastCalculatedAt,
    source,
    calculateGraphs,
    calculateDemoGraphs,
  } = useDashboardAnalytics();

  return (
    <main className="page-frame">
      <header className="page-header dashboard-brand-header">
        <div className="dashboard-title-group">
          <div className="dashboard-badge">AGILE-ish</div>
          <h2>Transaction & Alert Control Center</h2>
        </div>
        <p>Real-time transaction volume, rule evaluation metrics, and active alert lifecycle.</p>
      </header>

      {warning && (
        <p className="state-message" role="status">
          {warning}
        </p>
      )}

      <KpiStrip summary={summary} loading={loading} />

      {analyticsWarning && (
        <p className="state-message" role="status">
          {analyticsWarning}
        </p>
      )}

      <DashboardGraphs
        analytics={analytics}
        loading={analyticsLoading}
        lastCalculatedAt={lastCalculatedAt}
        source={source}
        infoMessage={analyticsInfo}
        onCalculate={calculateGraphs}
        onShowDemo={calculateDemoGraphs}
      />
    </main>
  );
}
