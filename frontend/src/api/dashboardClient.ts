import { apiGet } from './http';
import { demoAlerts, demoTransactions } from '../data/demoData';
import type { Alert } from '../types/alert';
import type { Transaction } from '../types/transaction';
import type { DashboardAnalytics, DashboardSummary, GraphPoint } from '../types/dashboard';

function toGraphPoints(counts: Record<string, number>): GraphPoint[] {
  return Object.entries(counts)
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value || a.label.localeCompare(b.label));
}

function countBy(values: string[]): Record<string, number> {
  return values.reduce<Record<string, number>>((acc, value) => {
    const key = value && value.trim() ? value : 'UNKNOWN';
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
}

/** Split multi-rule alerts so each rule type gets credit (matches backend). */
function ruleTypeTokens(alert: Alert): string[] {
  if (alert.ruleTypes && alert.ruleTypes.length > 0) {
    return alert.ruleTypes.map((token) => token.trim().toUpperCase()).filter(Boolean);
  }
  if (alert.ruleType) {
    return alert.ruleType
      .split(',')
      .map((token) => token.trim().toUpperCase())
      .filter(Boolean);
  }
  return ['UNKNOWN'];
}

function aggregateAnalytics(
  transactions: Transaction[],
  alerts: Alert[],
): DashboardAnalytics {
  return {
    transactionsByType: toGraphPoints(
      countBy(transactions.map((transaction) => transaction.transactionType)),
    ),
    transactionsByStatus: toGraphPoints(
      countBy(transactions.map((transaction) => transaction.status)),
    ),
    alertsByStatus: toGraphPoints(
      countBy(alerts.map((alert) => alert.status)),
    ),
    alertsBySeverity: toGraphPoints(
      countBy(alerts.map((alert) => alert.severity)),
    ),
    alertsByRuleType: toGraphPoints(
      countBy(alerts.flatMap((alert) => ruleTypeTokens(alert))),
    ),
  };
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const envelope = await apiGet<DashboardSummary>('/api/v1/dashboard');
  return envelope.data;
}

/** Full-table GROUP BY counts from the API — not a page sample. */
export async function getDashboardAnalytics(): Promise<DashboardAnalytics> {
  const envelope = await apiGet<DashboardAnalytics>('/api/v1/dashboard/analytics');
  return envelope.data;
}

export function getDemoDashboardAnalytics(): DashboardAnalytics {
  return aggregateAnalytics(demoTransactions, demoAlerts);
}
