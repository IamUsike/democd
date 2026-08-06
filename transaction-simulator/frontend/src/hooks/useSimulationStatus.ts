import { useEffect, useState } from "react";

import { getSimulationStatus } from "../services/simulatorService";
import type { SimulationStatus } from "../types/simulator";

interface UseSimulationStatusResult {
  status: SimulationStatus | null;
  loading: boolean;
  error: string;
}

export function useSimulationStatus(): UseSimulationStatusResult {
  const [status, setStatus] = useState<SimulationStatus | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    let isMounted = true;

    async function fetchStatus(): Promise<void> {
      try {
        const latestStatus = await getSimulationStatus();
        if (!isMounted) {
          return;
        }
        setStatus(latestStatus);
        setError("");
      } catch (fetchError: unknown) {
        if (!isMounted) {
          return;
        }
        if (fetchError instanceof Error) {
          setError(fetchError.message);
        } else {
          setError("Failed to fetch simulation status.");
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    void fetchStatus();
    const intervalId = window.setInterval(() => {
      void fetchStatus();
    }, 3000);

    return () => {
      isMounted = false;
      window.clearInterval(intervalId);
    };
  }, []);

  return { status, loading, error };
}

