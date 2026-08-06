import './App.css';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './layout/AppShell';
import { AlertsPage } from './pages/AlertsPage';
import { DashboardPage } from './pages/DashboardPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { RulesPage } from './pages/RulesPage';

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/transactions" element={<TransactionsPage />} />
        <Route path="/alerts" element={<AlertsPage />} />
        <Route path="/rules" element={<RulesPage />} />
      </Route>
    </Routes>
  );
}

export default App;
