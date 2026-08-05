export type RuleConfig = {
  ruleType: string;
  name: string;
  description: string;
  enabled: boolean;
  amountThreshold?: number | null;
  velocityMaxTransactions?: number | null;
  velocityWindowMinutes?: number | null;
  dailyLimit?: number | null;
  updatedAt: string;
};

export type RuleConfigUpdateRequest = {
  enabled?: boolean;
  amountThreshold?: number;
  velocityMaxTransactions?: number;
  velocityWindowMinutes?: number;
  dailyLimit?: number;
};

