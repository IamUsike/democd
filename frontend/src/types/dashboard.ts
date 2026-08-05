export type DashboardSummary = {
  totalTransactions: number;
  totalAlerts: number;
  openAlerts: number;
  closedAlerts: number;
  highSeverityAlerts: number;
};

export type GraphPoint = {
  label: string;
  value: number;
};

export type DashboardAnalytics = {
  transactionsByType: GraphPoint[];
  transactionsByStatus: GraphPoint[];
  alertsByStatus: GraphPoint[];
  alertsBySeverity: GraphPoint[];
};
