import { useCallback, useEffect, useState } from 'react';
import { getRules, updateRule } from '../api/rulesClient';
import type { RuleConfig, RuleConfigUpdateRequest } from '../types/rule';

export function useRules() {
  const [rules, setRules] = useState<RuleConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null); // ruleType being saved
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const items = await getRules();
        setRules(items);
      } catch {
        setError('Unable to load rules from the API.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const saveRule = useCallback(async (ruleType: string, payload: RuleConfigUpdateRequest) => {
    setSaving(ruleType);
    setError(null);
    setSuccessMsg(null);
    try {
      const updated = await updateRule(ruleType, payload);
      setRules((prev) => prev.map((r) => (r.ruleType === ruleType ? updated : r)));
      setSuccessMsg(`${updated.name} updated successfully.`);
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      const message = err instanceof Error ? err.message : `Failed to save changes for ${ruleType}.`;
      setError(message);
      throw err;
    } finally {
      setSaving(null);
    }
  }, []);

  return { rules, loading, saving, error, successMsg, saveRule };
}

