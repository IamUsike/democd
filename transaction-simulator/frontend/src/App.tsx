import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { SimulatorDashboardPage } from "./pages/SimulatorDashboardPage";

export default function App(): JSX.Element {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SimulatorDashboardPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

