import { NavLink } from "react-router-dom";

interface AppShellProps {
  children: React.ReactNode;
}

export function AppShell({ children }: AppShellProps): JSX.Element {
  return (
    <div className="app-shell">
      <header className="app-nav">
        <div className="app-brand">
          <span className="app-brand-mark">TxnMonitor</span>
          <span className="app-brand-title">Traffic Simulator</span>
        </div>
        <nav className="app-nav-links" aria-label="Primary">
          <NavLink
            to="/"
            end
            className={({ isActive }) => (isActive ? "app-nav-link active" : "app-nav-link")}
          >
            Simulator
          </NavLink>
          <NavLink
            to="/about"
            className={({ isActive }) => (isActive ? "app-nav-link active" : "app-nav-link")}
          >
            About
          </NavLink>
        </nav>
      </header>
      <div className="app-main">{children}</div>
    </div>
  );
}
