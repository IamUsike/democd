export type AlertStatus =
  | 'OPEN'
  | 'ACKNOWLEDGED'
  | 'INVESTIGATING'
  | 'CLOSED'
  | 'DISMISSED';

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export type Alert = {
  alertId: number;
  transactionId?: number;
  severity: AlertSeverity;
  status: AlertStatus;
  ruleTriggered: string;
  accountId?: string;
  sourceType?: 'BANK' | 'MERCHANT';
  sourceId?: string;
  sourceName?: string;
  createdAt: string;
  acknowledgedAt?: string | null;
  investigatingAt?: string | null;
  dismissedAt?: string | null;
  closedAt?: string | null;
  resolutionNotes?: string | null;
};

export type AlertStatusUpdateRequest = {
  status: AlertStatus;
  notes?: string;
};

