import { useState } from 'react';
import { Link } from 'react-router-dom';
import { get } from '../api';
import { ErrorBox, Loading, StatusBadge } from '../components/ui';
import { fmtDateTime } from '../format';
import { useLoad } from '../hooks';
import type { NurseAlert, Patient } from '../types';

export default function Alerts() {
  const [status, setStatus] = useState<'OPEN' | 'RESOLVED'>('OPEN');
  const alerts = useLoad(() => get<NurseAlert[]>(`/api/alerts?status=${status}`), [status], 20000);
  // Alerts carry ids only, so patient names are joined here from the patient list.
  const patients = useLoad(() => get<Patient[]>('/api/patients?careSetting=INPATIENT'), []);
  const nameOf = (id: string) => patients.data?.find((p) => p.id === id)?.fullName ?? 'Patient';

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Inpatient alerts</h1>
          <p className="muted">An alert opens when an inpatient dose is due and closes once a nurse records it. Refreshes every 20 seconds.</p>
        </div>
      </div>
      <div className="card">
        <div className="filters">
          <select value={status} onChange={(e) => setStatus(e.target.value as 'OPEN' | 'RESOLVED')}>
            <option value="OPEN">Open</option>
            <option value="RESOLVED">Resolved</option>
          </select>
          <button className="secondary small" onClick={alerts.reload}>Refresh</button>
        </div>
        <ErrorBox message={alerts.error} />
        <Loading loading={alerts.loading} />
        {alerts.data && (alerts.data.length === 0 ? <div className="empty">No {status.toLowerCase()} alerts.</div> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Patient</th><th>Due</th><th>Window closes</th><th>Status</th><th>Outcome</th></tr></thead>
              <tbody>
                {alerts.data.map((a) => (
                  <tr key={a.id}>
                    <td><Link to={`/patients/${a.patientId}`}>{nameOf(a.patientId)}</Link></td>
                    <td>{fmtDateTime(a.dueAt)}</td>
                    <td>{fmtDateTime(a.windowClosesAt)}</td>
                    <td><StatusBadge status={a.status} /></td>
                    <td>{a.resolvedStatus ? <StatusBadge status={a.resolvedStatus} /> : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </div>
  );
}
