export type PageResponse<T> = {
  items: T[];
  totalCount: number;
  page: number;
  size: number;
  hasNext: boolean;
};
