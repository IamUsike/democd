import { useMemo, useState } from "react";

import { StatusCard } from "../components";
import { useSimulationStatus } from "../hooks/useSimulationStatus";
import { startSimulation, stopSimulation } from "../services/simulatorService";
import {
  SCENARIO_PACKS,
  type ScenarioId,
  type SimulationMode,
  type SimulationRequest,
  type SourceType
} from "../types/simulator";

type ValidationErrors = {
  tps?: string;
  duration?: string;
  fraudMixPercent?: string;
};

const DEFAULT_TRAFFIC: Required<Pick<SimulationRequest, "tps" | "duration" | "mode">> & {
  fraudMixPercent: number;
  sourceType: "" | SourceType;
} = {
  tps: 50,
  duration: 30,
  mode: "NORMAL",
  fraudMixPercent: 0,
  sourceType: ""
};

function validateTraffic(request: {
  tps: number;
  duration: number;
  fraudMixPercent: number;
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

  return errors;
}

function toSafeNumber(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function SimulatorDashboardPage(): JSX.Element {
  const [tps, setTps] = useState<string>(String(DEFAULT_TRAFFIC.tps));
  const [duration, setDuration] = useState<string>(String(DEFAULT_TRAFFIC.duration));
  const [mode, setMode] = useState<SimulationMode>(DEFAULT_TRAFFIC.mode);
  const [sourceType, setSourceType] = useState<"" | SourceType>(DEFAULT_TRAFFIC.sourceType);
  const [fraudMixPercent, setFraudMixPercent] = useState<string>(
    String(DEFAULT_TRAFFIC.fraudMixPercent)
  );

  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [activeScenario, setActiveScenario] = useState<ScenarioId | null>(null);
  const [isStarting, setIsStarting] = useState<boolean>(false);
  const [isStopping, setIsStopping] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string>("");
  const [apiError, setApiError] = useState<string>("");

  const { status, loading: statusLoading, error: statusError } = useSimulationStatus();

  const isBusy = isStarting || isStopping || Boolean(status?.running);

  const trafficRequest = useMemo<SimulationRequest>(
    () => ({
      kind: "TRAFFIC",
      tps: toSafeNumber(tps),
      duration: toSafeNumber(duration),
      mode,
      sourceType: sourceType === "" ? null : sourceType,
      fraudMixPercent: toSafeNumber(fraudMixPercent)
    }),
    [tps, duration, mode, sourceType, fraudMixPercent]
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
      if (error instanceof Error) {
        setApiError(error.message);
      } else {
        setApiError("Failed to start simulation.");
      }
      setActiveScenario(null);
    } finally {
      setIsStarting(false);
    }
  }

  async function handleStartTraffic(): Promise<void> {
    const errors = validateTraffic({
      tps: toSafeNumber(tps),
      duration: toSafeNumber(duration),
      fraudMixPercent: toSafeNumber(fraudMixPercent)
    });
    setValidationErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }
    await runStart(trafficRequest, null);
  }

  async function handleStartScenario(scenario: ScenarioId): Promise<void> {
    setValidationErrors({});
    await runStart({ kind: "SCENARIO", scenario }, scenario);
  }

  async function handleStopSimulation(): Promise<void> {
    setSuccessMessage("");
    setApiError("");
    setValidationErrors({});
    setIsStopping(true);
    try {
      const message = await stopSimulation();
      setSuccessMessage(message);
      setActiveScenario(null);
    } catch (error: unknown) {
      if (error instanceof Error) {
        setApiError(error.message);
      } else {
        setApiError("Failed to stop simulation.");
      }
    } finally {
      setIsStopping(false);
    }
  }

  const simulationState = status?.running ? "RUNNING" : "STOPPED";
  const simulationStateStatus = status?.running ? "success" : "warning";
  const displayedScenario = status?.scenario || activeScenario || "—";
  const displayedKind = status?.kind || (status?.running ? "—" : "—");

  return (
    <main style={{ maxWidth: 960, margin: "0 auto", padding: "24px 16px 48px" }}>
      <h1 style={{ marginBottom: 4 }}>Simulator Dashboard</h1>
      <p style={{ marginTop: 0, color: "#4b5563" }}>
        Demo scenario packs for rule triggers, plus continuous TPS traffic. Alert lifecycle stays in
        the operator dashboard.
      </p>

      <section aria-label="Demo scenario packs" style={{ marginTop: 24 }}>
        <h2>Demo scenario packs</h2>
        <p style={{ color: "#6b7280" }}>
          One click posts a deterministic sequence to the monitoring API. Watch alerts open in the
          operator UI.
        </p>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
            gap: 12
          }}
        >
          {SCENARIO_PACKS.map((pack) => (
            <div
              key={pack.id}
              style={{
                border: "1px solid #e5e7eb",
                borderRadius: 8,
                padding: 12,
                display: "flex",
                flexDirection: "column",
                gap: 8
              }}
            >
              <strong>{pack.label}</strong>
              <span style={{ fontSize: "0.875rem", color: "#6b7280" }}>{pack.expected}</span>
              <button
                type="button"
                onClick={() => {
                  void handleStartScenario(pack.id);
                }}
                disabled={isBusy}
              >
                {isStarting && activeScenario === pack.id ? "Running…" : "Run pack"}
              </button>
            </div>
          ))}
        </div>
      </section>

      <section aria-label="Continuous traffic" style={{ marginTop: 32 }}>
        <h2>Continuous traffic</h2>
        <p style={{ color: "#6b7280" }}>
          NORMAL amounts stay under the default amount threshold. FRAUD emits full multi-txn
          sequences (velocity, daily limit, etc.).
        </p>

        <div style={{ display: "grid", gap: 12, maxWidth: 480 }}>
          <div>
            <label htmlFor="tps-input">TPS (transactions per second)</label>
            <input
              id="tps-input"
              type="number"
              min={1}
              step={1}
              value={tps}
              onChange={(event) => {
                setTps(event.target.value);
                setValidationErrors((previous) => ({ ...previous, tps: undefined }));
                setApiError("");
                setSuccessMessage("");
              }}
              disabled={isBusy}
            />
            {validationErrors.tps ? <p role="alert">{validationErrors.tps}</p> : null}
          </div>

          <div>
            <label htmlFor="duration-input">Duration (seconds)</label>
            <input
              id="duration-input"
              type="number"
              min={1}
              step={1}
              value={duration}
              onChange={(event) => {
                setDuration(event.target.value);
                setValidationErrors((previous) => ({ ...previous, duration: undefined }));
                setApiError("");
                setSuccessMessage("");
              }}
              disabled={isBusy}
            />
            {validationErrors.duration ? <p role="alert">{validationErrors.duration}</p> : null}
          </div>

          <div>
            <label htmlFor="mode-select">Simulation mode</label>
            <select
              id="mode-select"
              value={mode}
              onChange={(event) => {
                setMode(event.target.value as SimulationMode);
                setApiError("");
                setSuccessMessage("");
              }}
              disabled={isBusy}
            >
              <option value="NORMAL">NORMAL</option>
              <option value="FRAUD">FRAUD</option>
            </select>
          </div>

          <div>
            <label htmlFor="source-select">Source filter (optional)</label>
            <select
              id="source-select"
              value={sourceType}
              onChange={(event) => {
                setSourceType(event.target.value as "" | SourceType);
                setApiError("");
                setSuccessMessage("");
              }}
              disabled={isBusy}
            >
              <option value="">Any</option>
              <option value="BANK">BANK</option>
              <option value="MERCHANT">MERCHANT</option>
            </select>
          </div>

          <div>
            <label htmlFor="fraud-mix-input">Fraud mix % (when mode is NORMAL)</label>
            <input
              id="fraud-mix-input"
              type="number"
              min={0}
              max={100}
              step={1}
              value={fraudMixPercent}
              onChange={(event) => {
                setFraudMixPercent(event.target.value);
                setValidationErrors((previous) => ({ ...previous, fraudMixPercent: undefined }));
                setApiError("");
                setSuccessMessage("");
              }}
              disabled={isBusy}
            />
            {validationErrors.fraudMixPercent ? (
              <p role="alert">{validationErrors.fraudMixPercent}</p>
            ) : null}
          </div>

          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button type="button" onClick={() => void handleStartTraffic()} disabled={isBusy}>
              {isStarting && !activeScenario ? "Starting…" : "Start traffic"}
            </button>
            <button
              type="button"
              onClick={() => void handleStopSimulation()}
              disabled={isStarting || isStopping}
            >
              {isStopping ? "Stopping…" : "Stop"}
            </button>
          </div>
        </div>
      </section>

      {successMessage ? (
        <p role="status" style={{ marginTop: 16 }}>
          {successMessage}
        </p>
      ) : null}
      {apiError ? (
        <p role="alert" style={{ marginTop: 16, color: "#991b1b" }}>
          {apiError}
        </p>
      ) : null}

      <section aria-label="Live monitoring" style={{ marginTop: 32 }}>
        <h2>Live monitoring</h2>
        {statusLoading ? <p>Loading simulation status…</p> : null}
        {statusError ? <p role="alert">{statusError}</p> : null}

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
            gap: 12
          }}
        >
          <StatusCard title="Simulation status" value={simulationState} status={simulationStateStatus} />
          <StatusCard title="Kind" value={displayedKind} />
          <StatusCard title="Scenario" value={displayedScenario} />
          <StatusCard title="Mode" value={status?.mode || "—"} />
          <StatusCard title="Current TPS" value={status?.currentTPS ?? 0} />
          <StatusCard title="Generated" value={status?.transactionsGenerated ?? 0} />
          <StatusCard
            title="Successful"
            value={status?.successfulTransactions ?? 0}
            status="success"
          />
          <StatusCard
            title="Failed"
            value={status?.failedTransactions ?? 0}
            status={status && status.failedTransactions > 0 ? "error" : "normal"}
          />
        </div>
      </section>
    </main>
  );
}
