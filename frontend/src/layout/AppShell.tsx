import { NavLink, Outlet } from 'react-router-dom';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: '📊' },
  { to: '/transactions', label: 'Transactions', icon: '💳' },
  { to: '/alerts', label: 'Alerts', icon: '🚨' },
];

export function AppShell() {
  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="brand-header">
          <svg
            className="brand-logo"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <rect width="32" height="32" rx="10" fill="url(#logo-grad)" />
            <path
              d="M9 16L14 21L23 11"
              stroke="#FFFFFF"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <path
              d="M17 9L23 11L21 17"
              stroke="#A78BFA"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
                <stop stopColor="#6366F1" />
                <stop offset="0.5" stopColor="#8B5CF6" />
                <stop offset="1" stopColor="#10B981" />
              </linearGradient>
            </defs>
          </svg>
          <h1 className="app-name">AGILE-ish</h1>
        </div>

        <nav className="app-nav" aria-label="Primary">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <span className="nav-icon" aria-hidden="true">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="frontend-badge">
            <div className="badge-header">
              <span className="status-dot pulsing" aria-hidden="true" />
              <span className="badge-title">Frontend Console</span>
            </div>
            <p className="badge-desc">React 19 + Vite • TS</p>
            <span className="badge-pill">Soft Tenancy Mode</span>
          </div>
        </div>
      </aside>
      <div className="app-content">
        <Outlet />
      </div>
    </div>
  );
}

