export type SimulationMode = "NORMAL" | "FRAUD";

export interface SimulationRequest {
  tps: number;
  duration: number;
  mode: SimulationMode;
}

export interface SimulationStatus {
  running: boolean;
  transactionsGenerated: number;
  successfulTransactions: number;
  failedTransactions: number;
  currentTPS: number;
}

// Backward-compatible alias while transitioning naming to SimulationStatus.
export type SimulatorStatus = SimulationStatus;
