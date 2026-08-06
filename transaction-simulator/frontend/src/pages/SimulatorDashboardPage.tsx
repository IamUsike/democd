import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { useSimulationStatus } from "../hooks/useSimulationStatus";
import { useSimulatorApiUrl } from "../hooks/useSimulatorApiUrl";
import { startSimulation, stopSimulation } from "../services/simulatorService";
import {
  SCENARIO_PACKS,
  SELECTABLE_RULES,
  type RuleType,
  type ScenarioId,
  type SimulationMode,
  type SimulationRequest,
  type SourceType
} from "../types/simulator";

type ValidationErrors = {
  tps?: string;
  duration?: string;
  fraudMixPercent?: string;
  failedPercent?: string;
  multiRules?: string;
};

const DEFAULT_TRAFFIC = {
  tps: 50,
  duration: 30,
  mode: "NORMAL" as SimulationMode,
  fraudMixPercent: 0,
  failedPercent: 0,
  sourceType: "" as "" | SourceType
};

const DEFAULT_MULTI_RULES: RuleType[] = ["AMOUNT_THRESHOLD", "NEW_PAYEE"];

function validateTraffic(request: {
  tps: number;
  duration: number;
  fraudMixPercent: number;
  failedPercent: number;
}): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!Number.isFinite(request.tps) || request.tps <= 0) {
    errors.tps = "TPS must be greater than 0.";
  }
  if (!Number.isFinite(request.duration) || request.duration <= 0) {
    errors.duration = "Duration must be greater than 0.";
  }
  if (
    !Number.isFinite(request.fraudMixPercent) ||
    request.fraudMixPercent < 0 ||
    request.fraudMixPercent > 100
  ) {
    errors.fraudMixPercent = "Fraud mix must be between 0 and 100.";
  }
  if (
    !Number.isFinite(request.failedPercent) ||
    request.failedPercent < 0 ||
    request.failedPercent > 100
  ) {
    errors.failedPercent = "Failed % must be between 0 and 100.";
  }

  return errors;
}

function toSafeNumber(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function metricClass(kind: "ok" | "warn" | "danger" | "normal"): string {
  if (kind === "ok") return "metric-value is-ok";
  if (kind === "warn") return "metric-value is-warn";
  if (kind === "danger") return "metric-value is-danger";
  return "metric-value";
}

export function SimulatorDashboardPage(): JSX.Element {
  const apiUrl = useSimulatorApiUrl();
  const [tps, setTps] = useState(String(DEFAULT_TRAFFIC.tps));
  const [duration, setDuration] = useState(String(DEFAULT_TRAFFIC.duration));
  const [mode, setMode] = useState<SimulationMode>(DEFAULT_TRAFFIC.mode);
  const [sourceType, setSourceType] = useState<"" | SourceType>(DEFAULT_TRAFFIC.sourceType);
  const [fraudMixPercent, setFraudMixPercent] = useState(String(DEFAULT_TRAFFIC.fraudMixPercent));
  const [failedPercent, setFailedPercent] = useState(String(DEFAULT_TRAFFIC.failedPercent));
  const [multiRules, setMultiRules] = useState<RuleType[]>(DEFAULT_MULTI_RULES);

  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [activeScenario, setActiveScenario] = useState<ScenarioId | null>(null);
  const [isStarting, setIsStarting] = useState(false);
  const [isStopping, setIsStopping] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [apiError, setApiError] = useState("");

  const { status, loading: statusLoading, error: statusError } = useSimulationStatus();

  const isBusy = isStarting || isStopping || Boolean(status?.running);

  const trafficRequest = useMemo<SimulationRequest>(
    () => ({
      kind: "TRAFFIC",
      tps: toSafeNumber(tps),
      duration: toSafeNumber(duration),
      mode,
      sourceType: sourceType === "" ? null : sourceType,
      fraudMixPercent: toSafeNumber(fraudMixPercent),
      failedPercent: toSafeNumber(failedPercent)
    }),
    [tps, duration, mode, sourceType, fraudMixPercent, failedPercent]
  );

  async function runStart(request: SimulationRequest, scenarioHint: ScenarioId | null): Promise<void> {
    setSuccessMessage("");
    setApiError("");
    setIsStarting(true);
    setActiveScenario(scenarioHint);
    try {
      const message = await startSimulation(request);
      setSuccessMessage(message);
    } catch (error: unknown) {
      setApiError(error instanceof Error ? error.message : "Failed to start simulation.");
      setActiveScenario(null);
    } finally {
      setIsStarting(false);
    }
  }

  async function handleStartTraffic(): Promise<void> {
    const errors = validateTraffic({
      tps: toSafeNumber(tps),
      duration: toSafeNumber(duration),
      fraudMixPercent: toSafeNumber(fraudMixPercent),
      failedPercent: toSafeNumber(failedPercent)
    });
    setValidationErrors(errors);
    if (Object.keys(errors).length > 0) return;
    await runStart(trafficRequest, null);
  }

  async function handleStartScenario(scenario: ScenarioId): Promise<void> {
    setValidationErrors({});
    if (scenario === "MULTI_RULE") {
      if (multiRules.length < 2) {
        setValidationErrors({ multiRules: "Select at least two rules." });
        return;
      }
      await runStart({ kind: "SCENARIO", scenario, rules: multiRules }, scenario);
      return;
    }
    await runStart({ kind: "SCENARIO", scenario }, scenario);
  }

  function toggleMultiRule(rule: RuleType): void {
    setMultiRules((previous) => {
      if (previous.includes(rule)) {
        return previous.filter((item) => item !== rule);
      }
      return [...previous, rule];
    });
    setValidationErrors((prev) => ({ ...prev, multiRules: undefined }));
    setApiError("");
    setSuccessMessage("");
  }

  async function handleStopSimulation(): Promise<void> {
    setSuccessMessage("");
    setApiError("");
    setValidationErrors({});
    setIsStopping(true);
    try {
      setSuccessMessage(await stopSimulation());
      setActiveScenario(null);
    } catch (error: unknown) {
      setApiError(error instanceof Error ? error.message : "Failed to stop simulation.");
    } finally {
      setIsStopping(false);
    }
  }

  const running = Boolean(status?.running);
  const displayedScenario = status?.scenario || activeScenario || "—";
  const displayedKind = status?.kind || "—";
  const failed = status?.failedTransactions ?? 0;

  return (
    <>
      <header className="page-header">
        <h1>Run demo traffic</h1>
        <p>
          Fire a rule with one pack, or stream TPS traffic. Alerts land in the operator dashboard —{" "}
          <Link to="/about">how it works</Link>.
        </p>
      </header>

      <section className="metrics-strip" aria-label="Live monitoring">
        <article className="metric">
          <p className="metric-label">Status</p>
          <p className={metricClass(running ? "ok" : "warn")}>{running ? "RUNNING" : "STOPPED"}</p>
        </article>
        <article className="metric">
          <p className="metric-label">Kind / scenario</p>
          <p className="metric-value" title={`${displayedKind} · ${displayedScenario}`}>
            {displayedKind === "—" ? "—" : `${displayedKind}`}
            {displayedScenario !== "—" ? ` · ${displayedScenario}` : ""}
          </p>
        </article>
        <article className="metric">
          <p className="metric-label">Generated</p>
          <p className="metric-value">{status?.transactionsGenerated ?? (statusLoading ? "…" : 0)}</p>
        </article>
        <article className="metric">
          <p className="metric-label">OK / failed</p>
          <p className={metricClass(failed > 0 ? "danger" : "ok")}>
            {status?.successfulTransactions ?? 0}
            <span style={{ color: "var(--ink-muted)", fontWeight: 500 }}> / </span>
            {failed}
          </p>
        </article>
      </section>

      {statusError ? (
        <p className="banner banner-error" role="alert">
          Cannot reach simulator API at <code>{apiUrl || "(unset)"}</code>. Start the Go backend on
          port 8090, or check <code>VITE_SIMULATOR_API_URL</code>. ({statusError})
        </p>
      ) : null}

      <div className="layout-grid">
        <section className="panel panel-pad" aria-label="Demo scenario packs">
          <h2 className="panel-title">Scenario packs</h2>
          <p className="panel-hint">One click → deterministic sequence → expected OPEN alert.</p>
          <ul className="pack-list">
            {SCENARIO_PACKS.map((pack) => (
              <li key={pack.id} className="pack-row">
                <div className="pack-copy">
                  <p className="pack-name">{pack.label}</p>
                  <p className="pack-expected">{pack.expected}</p>
                  {pack.id === "MULTI_RULE" ? (
                    <div className="rule-checks" role="group" aria-label="Rules to combine">
                      {SELECTABLE_RULES.map((rule) => (
                        <label key={rule.id} className="rule-check">
                          <input
                            type="checkbox"
                            checked={multiRules.includes(rule.id)}
                            disabled={isBusy}
                            onChange={() => toggleMultiRule(rule.id)}
                          />
                          <span>{rule.label}</span>
                        </label>
                      ))}
                      {validationErrors.multiRules ? (
                        <p className="field-error">{validationErrors.multiRules}</p>
                      ) : null}
                    </div>
                  ) : null}
                </div>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={isBusy || (pack.id === "MULTI_RULE" && multiRules.length < 2)}
                  onClick={() => {
                    void handleStartScenario(pack.id);
                  }}
                >
                  {isStarting && activeScenario === pack.id ? "Running…" : "Run"}
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel panel-pad" aria-label="Continuous traffic">
          <h2 className="panel-title">Continuous traffic</h2>
          <p className="panel-hint">NORMAL stays quiet. FRAUD posts full multi-txn patterns.</p>

          <div className="form-grid">
            <div className="field">
              <label htmlFor="tps-input">TPS</label>
              <input
                id="tps-input"
                type="number"
                min={1}
                step={1}
                value={tps}
                disabled={isBusy}
                onChange={(event) => {
                  setTps(event.target.value);
                  setValidationErrors((prev) => ({ ...prev, tps: undefined }));
                  setApiError("");
                  setSuccessMessage("");
                }}
              />
              {validationErrors.tps ? <p className="field-error">{validationErrors.tps}</p> : null}
            </div>

            <div className="field">
              <label htmlFor="duration-input">Duration (s)</label>
              <input
                id="duration-input"
                type="number"
                min={1}
                step={1}
                value={duration}
                disabled={isBusy}
                onChange={(event) => {
                  setDuration(event.target.value);
                  setValidationErrors((prev) => ({ ...prev, duration: undefined }));
                  setApiError("");
                  setSuccessMessage("");
                }}
              />
              {validationErrors.duration ? (
                <p className="field-error">{validationErrors.duration}</p>
              ) : null}
            </div>

            <div className="field">
              <label htmlFor="mode-select">Mode</label>
              <select
                id="mode-select"
                value={mode}
                disabled={isBusy}
                onChange={(event) => {
                  setMode(event.target.value as SimulationMode);
                  setApiError("");
                  setSuccessMessage("");
                }}
              >
                <option value="NORMAL">NORMAL</option>
                <option value="FRAUD">FRAUD</option>
              </select>
            </div>

            <div className="field">
              <label htmlFor="source-select">Source</label>
              <select
                id="source-select"
                value={sourceType}
                disabled={isBusy}
                onChange={(event) => {
                  setSourceType(event.target.value as "" | SourceType);
                  setApiError("");
                  setSuccessMessage("");
                }}
              >
                <option value="">Any</option>
                <option value="BANK">BANK</option>
                <option value="MERCHANT">MERCHANT</option>
              </select>
            </div>

            <div className="field">
              <label htmlFor="fraud-mix-input">Fraud mix % (with NORMAL)</label>
              <input
                id="fraud-mix-input"
                type="number"
                min={0}
                max={100}
                step={1}
                value={fraudMixPercent}
                disabled={isBusy}
                onChange={(event) => {
                  setFraudMixPercent(event.target.value);
                  setValidationErrors((prev) => ({ ...prev, fraudMixPercent: undefined }));
                  setApiError("");
                  setSuccessMessage("");
                }}
              />
              {validationErrors.fraudMixPercent ? (
                <p className="field-error">{validationErrors.fraudMixPercent}</p>
              ) : null}
            </div>

            <div className="field">
              <label htmlFor="failed-pct-input">Failed txn % (status FAILED)</label>
              <input
                id="failed-pct-input"
                type="number"
                min={0}
                max={100}
                step={1}
                value={failedPercent}
                disabled={isBusy}
                onChange={(event) => {
                  setFailedPercent(event.target.value);
                  setValidationErrors((prev) => ({ ...prev, failedPercent: undefined }));
                  setApiError("");
                  setSuccessMessage("");
                }}
              />
              {validationErrors.failedPercent ? (
                <p className="field-error">{validationErrors.failedPercent}</p>
              ) : null}
            </div>
          </div>

          <div className="actions">
            <button
              type="button"
              className="btn btn-primary"
              disabled={isBusy}
              onClick={() => void handleStartTraffic()}
            >
              {isStarting && !activeScenario ? "Starting…" : "Start traffic"}
            </button>
            <button
              type="button"
              className="btn btn-danger"
              disabled={isStarting || isStopping}
              onClick={() => void handleStopSimulation()}
            >
              {isStopping ? "Stopping…" : "Stop"}
            </button>
          </div>

          {successMessage ? (
            <p className="banner banner-ok" role="status">
              {successMessage}
              {status?.currentTPS != null && status.currentTPS > 0
                ? ` · live TPS ${status.currentTPS}`
                : ""}
            </p>
          ) : null}
          {apiError ? (
            <p className="banner banner-error" role="alert">
              {apiError}
            </p>
          ) : null}
        </section>
      </div>
    </>
  );
}
