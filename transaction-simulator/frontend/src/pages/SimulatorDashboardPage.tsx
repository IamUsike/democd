import { useMemo, useState } from "react";

import { StatusCard } from "../components";
import { useSimulationStatus } from "../hooks/useSimulationStatus";
import { startSimulation, stopSimulation } from "../services/simulatorService";
import type { SimulationMode, SimulationRequest } from "../types/simulator";

type ValidationErrors = {
  tps?: string;
  duration?: string;
};

const DEFAULT_REQUEST: SimulationRequest = {
  tps: 100,
  duration: 30,
  mode: "NORMAL"
};

function validateRequest(request: SimulationRequest): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!Number.isFinite(request.tps) || request.tps <= 0) {
    errors.tps = "TPS must be greater than 0.";
  }

  if (!Number.isFinite(request.duration) || request.duration <= 0) {
    errors.duration = "Duration must be greater than 0.";
  }

  return errors;
}

function toSafeNumber(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function SimulatorDashboardPage(): JSX.Element {
  const [tps, setTps] = useState<string>(String(DEFAULT_REQUEST.tps));
  const [duration, setDuration] = useState<string>(String(DEFAULT_REQUEST.duration));
  const [mode, setMode] = useState<SimulationMode>(DEFAULT_REQUEST.mode);

  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [isStarting, setIsStarting] = useState<boolean>(false);
  const [isStopping, setIsStopping] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string>("");
  const [apiError, setApiError] = useState<string>("");

  const { status, loading: statusLoading, error: statusError } = useSimulationStatus();

  const isBusy = isStarting || isStopping;

  const request = useMemo<SimulationRequest>(
    () => ({
      tps: toSafeNumber(tps),
      duration: toSafeNumber(duration),
      mode
    }),
    [tps, duration, mode]
  );

  async function handleStartSimulation(): Promise<void> {
    setSuccessMessage("");
    setApiError("");

    const errors = validateRequest(request);
    setValidationErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    setIsStarting(true);
    try {
      const message = await startSimulation(request);
      setSuccessMessage(message);
    } catch (error: unknown) {
      if (error instanceof Error) {
        setApiError(error.message);
      } else {
        setApiError("Failed to start simulation.");
      }
    } finally {
      setIsStarting(false);
    }
  }

  async function handleStopSimulation(): Promise<void> {
    setSuccessMessage("");
    setApiError("");
    setValidationErrors({});

    setIsStopping(true);
    try {
      const message = await stopSimulation();
      setSuccessMessage(message);
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

  return (
    <main>
      <h1>Simulator Dashboard</h1>
      <p>Simulation Controls</p>

      <section aria-label="Simulator control panel">
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
          <button type="button" onClick={handleStartSimulation} disabled={isBusy}>
            {isStarting ? "Starting..." : "Start Simulation"}
          </button>
          <button type="button" onClick={handleStopSimulation} disabled={isBusy}>
            {isStopping ? "Stopping..." : "Stop Simulation"}
          </button>
        </div>

        {successMessage ? <p role="status">{successMessage}</p> : null}
        {apiError ? <p role="alert">{apiError}</p> : null}
      </section>

      <section aria-label="Live monitoring" style={{ marginTop: "24px" }}>
        <h2>Live Monitoring</h2>
        {statusLoading ? <p>Loading simulation status...</p> : null}
        {statusError ? <p role="alert">{statusError}</p> : null}

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
            gap: "12px"
          }}
        >
          <StatusCard title="Simulation Status" value={simulationState} status={simulationStateStatus} />
          <StatusCard title="Current TPS" value={status?.currentTPS ?? 0} />
          <StatusCard title="Transactions Generated" value={status?.transactionsGenerated ?? 0} />
          <StatusCard title="Successful Transactions" value={status?.successfulTransactions ?? 0} status="success" />
          <StatusCard title="Failed Transactions" value={status?.failedTransactions ?? 0} status={status && status.failedTransactions > 0 ? "error" : "normal"} />
        </div>
      </section>
    </main>
  );
}
