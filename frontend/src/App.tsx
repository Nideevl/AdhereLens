import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import Layout from './components/Layout';
import Alerts from './pages/Alerts';
import DrugRules from './pages/DrugRules';
import Hospitals from './pages/Hospitals';
import Login from './pages/Login';
import PatientDetail from './pages/PatientDetail';
import Patients from './pages/Patients';
import Staff from './pages/Staff';
import Worklist from './pages/Worklist';
import type { Role } from './types';

const HOME: Record<Role, string> = {
  PLATFORM_ADMIN: '/hospitals',
  HOSPITAL_ADMIN: '/staff',
  DOCTOR: '/patients',
  NURSE: '/doses',
};

/** Shows the page only to the listed roles; anyone else goes to their own home page. */
function Only({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" replace />;
  if (!roles.includes(auth.role)) return <Navigate to={HOME[auth.role]} replace />;
  return <>{children}</>;
}

export default function App() {
  const { auth } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={auth ? <Navigate to={HOME[auth.role]} replace /> : <Login />} />
      <Route element={auth ? <Layout /> : <Navigate to="/login" replace />}>
        <Route path="/hospitals" element={<Only roles={['PLATFORM_ADMIN']}><Hospitals /></Only>} />
        <Route path="/staff" element={<Only roles={['HOSPITAL_ADMIN']}><Staff /></Only>} />
        <Route path="/patients" element={<Only roles={['HOSPITAL_ADMIN', 'DOCTOR', 'NURSE']}><Patients /></Only>} />
        <Route path="/patients/:id" element={<Only roles={['HOSPITAL_ADMIN', 'DOCTOR', 'NURSE']}><PatientDetail /></Only>} />
        <Route path="/doses" element={<Only roles={['HOSPITAL_ADMIN', 'DOCTOR', 'NURSE']}><Worklist /></Only>} />
        <Route path="/alerts" element={<Only roles={['DOCTOR', 'NURSE']}><Alerts /></Only>} />
        <Route path="/drug-rules" element={<Only roles={['HOSPITAL_ADMIN', 'DOCTOR', 'NURSE']}><DrugRules /></Only>} />
      </Route>
      <Route path="*" element={<Navigate to={auth ? HOME[auth.role] : '/login'} replace />} />
    </Routes>
  );
}
