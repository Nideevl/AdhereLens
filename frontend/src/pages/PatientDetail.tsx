import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { get, put } from '../api';
import { useAuth } from '../auth';
import PatientForm from '../components/PatientForm';
import RegimenPanel from '../components/RegimenPanel';
import SideEffectPanel from '../components/SideEffectPanel';
import { ErrorBox, Loading, StatusBadge } from '../components/ui';
import { ageYears, fmtDate, label } from '../format';
import { useLoad } from '../hooks';
import type { Patient, PatientInput, Regimen } from '../types';

export default function PatientDetail() {
  const { id } = useParams<{ id: string }>();
  const { auth } = useAuth();
  const role = auth?.role;
  const patient = useLoad(() => get<Patient>(`/api/patients/${id}`), [id]);
  const regimens = useLoad(() => get<Regimen[]>(`/api/patients/${id}/regimens`), [id]);
  const [editing, setEditing] = useState(false);

  async function save(input: PatientInput) {
    await put(`/api/patients/${id}`, input);
    setEditing(false);
    patient.reload();
  }

  if (patient.error) {
    return (
      <div>
        <Link to="/patients">Back to patients</Link>
        <ErrorBox message={patient.error} />
      </div>
    );
  }
  if (!patient.data) return <Loading loading />;
  const p = patient.data;

  return (
    <div>
      <p><Link to="/patients">Back to patients</Link></p>
      <div className="card">
        <div className="page-head" style={{ marginBottom: 0 }}>
          <div>
            <h1>{p.fullName}</h1>
            <div className="kv">
              <span>{ageYears(p.dateOfBirth)} years ({fmtDate(p.dateOfBirth)})</span>
              <span>{label(p.gender)}</span>
              <StatusBadge status={p.careSetting} />
              <span>{label(p.conditionCode)}</span>
              <span>{p.weightKg != null ? `${p.weightKg} kg` : 'Weight not recorded'}</span>
              {p.phone && <span>Phone {p.phone}</span>}
              {p.guardianPhone && <span>Guardian {p.guardianPhone}</span>}
              {!p.active && <span className="badge red">Inactive</span>}
            </div>
          </div>
          {(role === 'HOSPITAL_ADMIN' || role === 'DOCTOR') && (
            <button className="secondary" onClick={() => setEditing(!editing)}>{editing ? 'Cancel' : 'Edit patient'}</button>
          )}
        </div>
      </div>

      {editing && <PatientForm initial={p} submitText="Save" onSubmit={save} onCancel={() => setEditing(false)} />}

      <ErrorBox message={regimens.error} />
      {regimens.data && (
        <>
          <RegimenPanel
            patient={p}
            regimens={regimens.data}
            canPrescribe={role === 'DOCTOR'}
            canEdit={role === 'DOCTOR' || role === 'NURSE'}
            canDiscontinue={role === 'DOCTOR'}
            onChanged={regimens.reload}
          />
          <SideEffectPanel
            patient={p}
            regimens={regimens.data}
            canReport={role === 'DOCTOR' || role === 'NURSE'}
            canRetract={role === 'DOCTOR'}
          />
        </>
      )}
    </div>
  );
}
