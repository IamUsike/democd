import { apiGet } from './http';
import { getAlerts } from './alertsClient';
import { getTransactions } from './transactionsClient';
import { demoAlerts, demoTransactions } from '../data/demoData';
import type { Alert } from '../types/alert';
import type { Transaction } from '../types/transaction';
import type { DashboardAnalytics, DashboardSummary, GraphPoint } from '../types/dashboard';

function toGraphPoints(counts: Record<string, number>): GraphPoint[] {
  return Object.entries(counts)
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value);
}

function countBy(values: string[]): Record<string, number> {
  return values.reduce<Record<string, number>>((acc, value) => {
    const key = value && value.trim() ? value : 'UNKNOWN';
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
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
  };
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const envelope = await apiGet<DashboardSummary>('/api/v1/dashboard');
  return envelope.data;
}

export async function getDashboardAnalytics(): Promise<DashboardAnalytics> {
  const [transactions, alerts] = await Promise.all([
    getTransactions({}),
    getAlerts(),
  ]);

  return aggregateAnalytics(transactions, alerts);
}

export function getDemoDashboardAnalytics(): DashboardAnalytics {
  return aggregateAnalytics(demoTransactions, demoAlerts);
}
