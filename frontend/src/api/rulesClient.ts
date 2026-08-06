import { apiGet, apiPut } from './http';
import type { RuleConfig, RuleConfigUpdateRequest } from '../types/rule';

export async function getRules(): Promise<RuleConfig[]> {
  const envelope = await apiGet<RuleConfig[]>('/api/v1/rules');
  return envelope.data;
}

export async function getRuleByType(ruleType: string): Promise<RuleConfig> {
  const envelope = await apiGet<RuleConfig>(`/api/v1/rules/${ruleType}`);
  return envelope.data;
}

export async function updateRule(
  ruleType: string,
  payload: RuleConfigUpdateRequest,
): Promise<RuleConfig> {
  const envelope = await apiPut<RuleConfigUpdateRequest, RuleConfig>(
    `/api/v1/rules/${ruleType}`,
    payload,
  );
  return envelope.data;
}

