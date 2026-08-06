export type AlertStatus =
  | 'OPEN'
  | 'ACKNOWLEDGED'
  | 'INVESTIGATING'
  | 'CLOSED'
  | 'DISMISSED';

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export type Alert = {
  alertId: number;
  transactionId?: number | null;
  transactionIds?: number[];
  severity: AlertSeverity;
  status: AlertStatus;
  ruleType?: string;
  ruleTriggered: string;
  accountId?: string | null;
  sourceType?: 'BANK' | 'MERCHANT' | null;
  sourceId?: string | null;
  sourceName?: string | null;
  createdAt: string;
  acknowledgedAt?: string | null;
  investigatingAt?: string | null;
  dismissedAt?: string | null;
  closedAt?: string | null;
  resolutionNotes?: string | null;
  ruleDescription?: string | null;
  failingReason?: string | null;
};

export type AlertStatusUpdateRequest = {
  status: AlertStatus;
  notes?: string;
};

export type AlertFilters = {
  sourceType?: 'BANK' | 'MERCHANT' | '';
  sourceId?: string;
  status?: AlertStatus | 'ALL' | '';
  severity?: AlertSeverity | '';
  accountId?: string;
  q?: string;
  sort?: string;
  page?: number;
  size?: number;
};
