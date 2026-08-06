export type SourceType = 'BANK' | 'MERCHANT';

export type Transaction = {
  transactionId: number;
  sourceType: SourceType;
  sourceId: string;
  sourceName: string;
  accountId: string;
  payeeId: string;
  payeeName?: string | null;
  amount: number;
  currency: string;
  transactionType: string;
  timestamp: string;
  location?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  description?: string | null;
  status: string;
};

export type TransactionFilters = {
  sourceType?: SourceType | '';
  sourceId?: string;
  accountId?: string;
  q?: string;
  sort?: string;
  page?: number;
  size?: number;
};
