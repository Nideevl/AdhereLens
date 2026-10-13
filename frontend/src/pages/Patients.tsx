import { useState } from 'react';
import { Link } from 'react-router-dom';
import { get, post } from '../api';
import { useAuth } from '../auth';
import PatientForm from '../components/PatientForm';
import { ErrorBox, Loading, StatusBadge } from '../components/ui';
import { ageYears, label } from '../format';
import { useLoad } from '../hooks';
import type { CareSetting, Patient, PatientInput } from '../types';

export default function Patients() {
  const { auth } = useAuth();
  const canCreate = auth?.role === 'HOSPITAL_ADMIN' || auth?.role === 'DOCTOR';
  const [setting, setSetting] = useState<CareSetting | ''>('');
  const [search, setSearch] = useState('');
  const [adding, setAdding] = useState(false);
  const { data, error, loading, reload } = useLoad(
    () => get<Patient[]>(`/api/patients${setting ? `?careSetting=${setting}` : ''}`),
    [setting],
  );

  async function create(input: PatientInput) {
    await post('/api/patients', input);
    setAdding(false);
    reload();
  }

  const rows = (data ?? []).filter((p) => p.fullName.toLowerCase().includes(search.trim().toLowerCase()));

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Patients</h1>
          <p className="muted">Patients of your hospital only.</p>
        </div>
        {canCreate && <button onClick={() => setAdding(!adding)}>{adding ? 'Cancel' : 'Add patient'}</button>}
      </div>

      {adding && <PatientForm submitText="Add patient" onSubmit={create} onCancel={() => setAdding(false)} />}

      <div className="card">
        <div className="filters">
          <input placeholder="Search by name" value={search} onChange={(e) => setSearch(e.target.value)} />
          <select value={setting} onChange={(e) => setSetting(e.target.value as CareSetting | '')}>
            <option value="">All settings</option>
            <option value="INPATIENT">Inpatient</option>
            <option value="OUTPATIENT">Outpatient</option>
          </select>
        </div>
        <ErrorBox message={error} />
        <Loading loading={loading} />
        {data && (rows.length === 0 ? <div className="empty">No patients found.</div> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Name</th><th>Age</th><th>Gender</th><th>Setting</th><th>Condition</th><th>Weight</th></tr></thead>
              <tbody>
                {rows.map((p) => (
                  <tr key={p.id}>
                    <td><Link to={`/patients/${p.id}`}>{p.fullName}</Link></td>
                    <td>{ageYears(p.dateOfBirth)}</td>
                    <td>{label(p.gender)}</td>
                    <td><StatusBadge status={p.careSetting} /></td>
                    <td>{label(p.conditionCode)}</td>
                    <td>{p.weightKg != null ? `${p.weightKg} kg` : <span className="muted">not recorded</span>}</td>
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
