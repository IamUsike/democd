import axios, { type AxiosError } from "axios";

import { apiClient } from "./apiClient";
import type { SimulationRequest, SimulationStatus } from "../types/simulator";

interface MessageResponse {
  message?: string;
}

interface ErrorResponse {
  error?: string;
  message?: string;
}

export class SimulatorServiceError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "SimulatorServiceError";
    this.status = status;
  }
}

function normalizeServiceError(error: unknown, fallbackMessage: string): SimulatorServiceError {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponse>;
    const status = axiosError.response?.status;
    const apiMessage = axiosError.response?.data?.error ?? axiosError.response?.data?.message;
    const message = apiMessage ?? axiosError.message ?? fallbackMessage;
    return new SimulatorServiceError(message, status);
  }

  if (error instanceof Error) {
    return new SimulatorServiceError(error.message);
  }

  return new SimulatorServiceError(fallbackMessage);
}

export async function startSimulation(request: SimulationRequest): Promise<string> {
  try {
    const response = await apiClient.post<MessageResponse>("/api/simulator/start", request);
    return response.data.message ?? "simulation started";
  } catch (error: unknown) {
    throw normalizeServiceError(error, "Failed to start simulation");
  }
}

export async function stopSimulation(): Promise<string> {
  try {
    const response = await apiClient.post<MessageResponse>("/api/simulator/stop");
    return response.data.message ?? "simulation stopped";
  } catch (error: unknown) {
    throw normalizeServiceError(error, "Failed to stop simulation");
  }
}

export async function getSimulationStatus(): Promise<SimulationStatus> {
  try {
    const response = await apiClient.get<SimulationStatus>("/api/simulator/status");
    return response.data;
  } catch (error: unknown) {
    throw normalizeServiceError(error, "Failed to fetch simulation status");
  }
}

