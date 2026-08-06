import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { AppShell } from "./layout/AppShell";
import { AboutPage } from "./pages/AboutPage";
import { SimulatorDashboardPage } from "./pages/SimulatorDashboardPage";

export default function App(): JSX.Element {
  return (
    <BrowserRouter>
      <AppShell>
        <Routes>
          <Route path="/" element={<SimulatorDashboardPage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AppShell>
    </BrowserRouter>
  );
}
