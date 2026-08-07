export type SimulationKind = "TRAFFIC" | "SCENARIO";

export type SimulationMode = "NORMAL" | "FRAUD";

export type SourceType = "BANK" | "MERCHANT";

export type ScenarioId =
  | "AMOUNT_THRESHOLD"
  | "VELOCITY"
  | "NEW_PAYEE"
  | "DAILY_LIMIT"
  | "SOFT_TENANCY_MIX"
  | "MVP_SEED"
  | "MULTI_RULE";

export type RuleType =
  | "AMOUNT_THRESHOLD"
  | "VELOCITY"
  | "NEW_PAYEE"
  | "DAILY_LIMIT";

export const SELECTABLE_RULES: { id: RuleType; label: string }[] = [
  { id: "AMOUNT_THRESHOLD", label: "Amount threshold" },
  { id: "VELOCITY", label: "Velocity" },
  { id: "NEW_PAYEE", label: "New payee" },
  { id: "DAILY_LIMIT", label: "Daily limit" }
];

export interface SimulationRequest {
  kind?: SimulationKind;
  tps?: number;
  duration?: number;
  mode?: SimulationMode;
  scenario?: ScenarioId;
  rules?: RuleType[];
  sourceType?: SourceType | null;
  fraudMixPercent?: number | null;
  failedPercent?: number | null;
}

export interface SimulationStatus {
  running: boolean;
  kind?: string;
  scenario?: string;
  mode?: string;
  transactionsGenerated: number;
  successfulTransactions: number;
  failedTransactions: number;
  currentTPS: number;
}

export type SimulatorStatus = SimulationStatus;

export interface ScenarioPackMeta {
  id: ScenarioId;
  label: string;
  expected: string;
}

export const SCENARIO_PACKS: ScenarioPackMeta[] = [
  {
    id: "AMOUNT_THRESHOLD",
    label: "Amount threshold",
    expected: "OPEN alert — AMOUNT_THRESHOLD"
  },
  {
    id: "VELOCITY",
    label: "Velocity burst",
    expected: "OPEN alert — VELOCITY"
  },
  {
    id: "NEW_PAYEE",
    label: "New payee",
    expected: "OPEN alert — NEW_PAYEE (fresh payee id every run)"
  },
  {
    id: "DAILY_LIMIT",
    label: "Daily limit",
    expected: "OPEN alert — DAILY_LIMIT"
  },
  {
    id: "MULTI_RULE",
    label: "Multi-rule hit",
    expected: "OPEN alert — pick ≥2 rules below"
  },
  {
    id: "SOFT_TENANCY_MIX",
    label: "Soft tenancy mix",
    expected: "No alert — BANK + MERCHANT (seeded pairs)"
  },
  {
    id: "MVP_SEED",
    label: "MVP seed path",
    expected: "OPEN alert — AMOUNT_THRESHOLD only (3 txns)"
  }
];
