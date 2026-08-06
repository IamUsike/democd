import { useEffect, useState } from 'react';
import { useRules } from '../hooks/useRules';
import type { RuleConfig, RuleConfigUpdateRequest } from '../types/rule';

/* ── per-rule metadata ─────────────────────────────────────────────────── */

const RULE_ICONS: Record<string, string> = {
  AMOUNT_THRESHOLD: '💰',
  VELOCITY: '⚡',
  NEW_PAYEE: '👤',
  DAILY_LIMIT: '📊',
};

const SEVERITY_META: Record<string, { label: string; cls: string }> = {
  AMOUNT_THRESHOLD: { label: 'MEDIUM', cls: 'severity medium' },
  VELOCITY:         { label: 'LOW',    cls: 'severity low'    },
  NEW_PAYEE:        { label: 'LOW',    cls: 'severity low'    },
  DAILY_LIMIT:      { label: 'MEDIUM', cls: 'severity medium' },
};

/* ── RuleCard ──────────────────────────────────────────────────────────── */

function RuleCard({
  rule,
  saving,
  onSave,
}: {
  rule: RuleConfig;
  saving: boolean;
  onSave: (ruleType: string, payload: RuleConfigUpdateRequest) => Promise<void>;
}) {
  const [enabled, setEnabled]               = useState(rule.enabled);
  const [amountThreshold, setAmountThreshold] = useState(
    rule.amountThreshold != null ? String(rule.amountThreshold) : '',
  );
  const [velocityMax, setVelocityMax]       = useState(
    rule.velocityMaxTransactions != null ? String(rule.velocityMaxTransactions) : '',
  );
  const [velocityWindow, setVelocityWindow] = useState(
    rule.velocityWindowMinutes != null ? String(rule.velocityWindowMinutes) : '',
  );
  const [dailyLimit, setDailyLimit]         = useState(
    rule.dailyLimit != null ? String(rule.dailyLimit) : '',
  );
  const [localError, setLocalError]         = useState<string | null>(null);

  /* BUG 2 FIX — sync form state from server-canonical values after a save */
  useEffect(() => {
    setEnabled(rule.enabled);
    setAmountThreshold(rule.amountThreshold != null ? String(rule.amountThreshold) : '');
    setVelocityMax(rule.velocityMaxTransactions != null ? String(rule.velocityMaxTransactions) : '');
    setVelocityWindow(rule.velocityWindowMinutes != null ? String(rule.velocityWindowMinutes) : '');
    setDailyLimit(rule.dailyLimit != null ? String(rule.dailyLimit) : '');
  }, [rule]);

  /* Toggle auto-saves; revert local state if the API call fails */
  async function handleToggle(next: boolean) {
    const previous = enabled;
    setEnabled(next);
    try {
      await onSave(rule.ruleType, { enabled: next });
    } catch {
      setEnabled(previous);
    }
  }

  /* BUG 3 FIX — validate inputs before sending */
  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLocalError(null);

    const payload: RuleConfigUpdateRequest = { enabled };

    if (rule.ruleType === 'AMOUNT_THRESHOLD') {
      if (amountThreshold !== '') {
        const val = parseFloat(amountThreshold);
        if (isNaN(val) || val <= 0) {
          setLocalError('Amount threshold must be a positive number.');
          return;
        }
        payload.amountThreshold = val;
      }
    }

    if (rule.ruleType === 'VELOCITY') {
      if (velocityMax !== '') {
        const val = parseInt(velocityMax, 10);
        if (isNaN(val) || val < 1) {
          setLocalError('Max transactions must be a positive whole number.');
          return;
        }
        payload.velocityMaxTransactions = val;
      }
      if (velocityWindow !== '') {
        const val = parseInt(velocityWindow, 10);
        if (isNaN(val) || val < 1) {
          setLocalError('Window minutes must be a positive whole number.');
          return;
        }
        payload.velocityWindowMinutes = val;
      }
    }

    if (rule.ruleType === 'DAILY_LIMIT') {
      if (dailyLimit !== '') {
        const val = parseFloat(dailyLimit);
        if (isNaN(val) || val <= 0) {
          setLocalError('Daily limit must be a positive number.');
          return;
        }
        payload.dailyLimit = val;
      }
    }

    onSave(rule.ruleType, payload);
  }

  const sev     = SEVERITY_META[rule.ruleType];
  const icon    = RULE_ICONS[rule.ruleType] ?? '⚙️';
  const hasParams = rule.ruleType !== 'NEW_PAYEE';

  return (
    <div className={`rule-card card${!enabled ? ' rule-card--disabled' : ''}`}>
      {/* ── header row: badge + severity + toggle ── */}
      <div className="rule-card-top">
        <div className="rule-card-meta">
          <span className="rule-icon">{icon}</span>
          <span className="rule-type-badge">{rule.ruleType.replace(/_/g, ' ')}</span>
          {sev && <span className={sev.cls}>{sev.label}</span>}
        </div>

        <label className="rule-toggle" title={enabled ? 'Click to disable' : 'Click to enable'}>
          <input
            type="checkbox"
            checked={enabled}
            onChange={(e) => handleToggle(e.target.checked)}
            className="rule-toggle-input"
            disabled={saving}
            aria-label={`${rule.name} ${enabled ? 'enabled' : 'disabled'}`}
          />
          <span className={`rule-toggle-track${enabled ? ' rule-toggle-track--on' : ''}`} aria-hidden="true" />
          <span className="rule-toggle-label">{enabled ? 'Enabled' : 'Disabled'}</span>
        </label>
      </div>

      {/* ── name + description ── */}
      <div className="rule-card-body">
        <h3 className="rule-name">{rule.name}</h3>
        <p className="rule-description">{rule.description}</p>
      </div>

      {/* ── validation error ── */}
      {localError && (
        <p className="rule-local-error" role="alert">{localError}</p>
      )}

      {/* ── parameter form ── */}
      <form onSubmit={handleSubmit}>
        {rule.ruleType === 'AMOUNT_THRESHOLD' && (
          <div className="rule-params">
            <label className="rule-param-label">
              Threshold Amount (USD)
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={amountThreshold}
                onChange={(e) => { setAmountThreshold(e.target.value); setLocalError(null); }}
                placeholder="e.g. 10000"
                className="rule-param-input"
              />
            </label>
          </div>
        )}

        {rule.ruleType === 'VELOCITY' && (
          <div className="rule-params rule-params-row">
            <label className="rule-param-label">
              Max Transactions
              <input
                type="number"
                min="1"
                step="1"
                value={velocityMax}
                onChange={(e) => { setVelocityMax(e.target.value); setLocalError(null); }}
                placeholder="e.g. 5"
                className="rule-param-input"
              />
            </label>
            <label className="rule-param-label">
              Window (minutes)
              <input
                type="number"
                min="1"
                step="1"
                value={velocityWindow}
                onChange={(e) => { setVelocityWindow(e.target.value); setLocalError(null); }}
                placeholder="e.g. 10"
                className="rule-param-input"
              />
            </label>
          </div>
        )}

        {rule.ruleType === 'DAILY_LIMIT' && (
          <div className="rule-params">
            <label className="rule-param-label">
              Daily Limit Amount (USD)
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={dailyLimit}
                onChange={(e) => { setDailyLimit(e.target.value); setLocalError(null); }}
                placeholder="e.g. 50000"
                className="rule-param-input"
              />
            </label>
          </div>
        )}

        {rule.ruleType === 'NEW_PAYEE' && (
          <div className="rule-no-params">
            <span className="muted">No configurable parameters — use the toggle above to enable or disable this rule.</span>
          </div>
        )}

        <div className="rule-card-footer">
          <span className="rule-updated muted">
            Updated: {new Date(rule.updatedAt).toLocaleString()}
          </span>
          {hasParams && (
            <button type="submit" className="rule-save-btn" disabled={saving}>
              {saving ? 'Saving…' : 'Save Parameters'}
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

/* ── RulesPage ─────────────────────────────────────────────────────────── */

export function RulesPage() {
  const { rules, loading, saving, error, successMsg, saveRule } = useRules();

  return (
    <main className="page-frame">
      <header className="page-header">
        <h2>Monitoring Rules</h2>
        <p>Toggle and configure the parameters for each rule that evaluates incoming transactions in real time.</p>
      </header>

      {error      && <p className="state-message rule-error"   role="alert">{error}</p>}
      {successMsg && <p className="state-message rule-success" role="status">{successMsg}</p>}

      {loading ? (
        <p className="state-message">Loading rules…</p>
      ) : rules.length === 0 ? (
        <p className="state-message muted">No rules configured.</p>
      ) : (
        <div className="rules-grid">
          {rules.map((rule) => (
            <RuleCard
              key={rule.ruleType}
              rule={rule}
              saving={saving === rule.ruleType}
              onSave={saveRule}
            />
          ))}
        </div>
      )}
    </main>
  );
}
