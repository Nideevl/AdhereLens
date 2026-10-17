import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth';
import { label } from '../format';
import type { Role } from '../types';

const NAV: Record<Role, { to: string; text: string }[]> = {
  PLATFORM_ADMIN: [{ to: '/hospitals', text: 'Hospitals' }, { to: '/analytics', text: 'Analytics' }],
  HOSPITAL_ADMIN: [
    { to: '/staff', text: 'Staff' },
    { to: '/patients', text: 'Patients' },
    { to: '/doses', text: 'Doses' },
    { to: '/analytics', text: 'Analytics' },
    { to: '/drug-rules', text: 'Drug rules' },
  ],
  DOCTOR: [
    { to: '/patients', text: 'Patients' },
    { to: '/doses', text: 'Doses' },
    { to: '/alerts', text: 'Alerts' },
    { to: '/analytics', text: 'Analytics' },
    { to: '/drug-rules', text: 'Drug rules' },
  ],
  NURSE: [
    { to: '/doses', text: 'Doses' },
    { to: '/alerts', text: 'Alerts' },
    { to: '/patients', text: 'Patients' },
    { to: '/drug-rules', text: 'Drug rules' },
  ],
};

export default function Layout() {
  const { auth, logout } = useAuth();
  if (!auth) return null;
  return (
    <div>
      <header className="topbar">
        <div className="brand">AdhereLens</div>
        <nav>
          {NAV[auth.role].map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {item.text}
            </NavLink>
          ))}
        </nav>
        <div className="who">
          <span>{auth.fullName}</span>
          <span className="badge teal">{label(auth.role)}</span>
          <button className="secondary small" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  );
}
