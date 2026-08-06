import axios, { type AxiosInstance } from "axios";

const simulatorApiBaseUrl = import.meta.env.VITE_SIMULATOR_API_URL;

if (!simulatorApiBaseUrl) {
  throw new Error("Missing required environment variable: VITE_SIMULATOR_API_URL");
}

export const apiClient: AxiosInstance = axios.create({
  baseURL: simulatorApiBaseUrl,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json"
  }
});

