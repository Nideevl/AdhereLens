import { useState } from 'react';
import { Link } from 'react-router-dom';
import { get, post } from '../api';
import { useAuth } from '../auth';
import { ErrorBox, Loading, StatusBadge } from '../components/ui';
import { fmtDateTime, label } from '../format';
import { useLoad } from '../hooks';
import type { CareSetting, DoseSlot, SlotStatus } from '../types';

const STATUSES: SlotStatus[] = ['PENDING', 'HIT', 'DELAYED', 'MISSED', 'REFUSED'];

function minutesLeft(iso: string): number {
  return Math.floor((new Date(iso).getTime() - Date.now()) / 60000);
}

export default function Worklist() {
  const { auth } = useAuth();
  const canRecord = auth?.role === 'NURSE' || auth?.role === 'DOCTOR';
  const [status, setStatus] = useState<SlotStatus>('PENDING');
  const [setting, setSetting] = useState<CareSetting | ''>('');
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const query = `status=${status}${setting ? `&careSetting=${setting}` : ''}`;
  const { data, error, loading, reload } = useLoad(() => get<DoseSlot[]>(`/api/dose-slots?${query}`), [query], 20000);

  async function record(slot: DoseSlot, outcome: 'ADMINISTERED' | 'REFUSED') {
    setActionError(null);
    setBusyId(slot.id);
    try {
      await post(`/api/dose-slots/${slot.id}/record`, { outcome });
      reload();
    } catch (err) {
      setActionError((err as Error).message);
      reload();
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Doses</h1>
          <p className="muted">
            Doses that are due now. Anything not recorded before its window closes is marked Missed automatically. Refreshes every 20 seconds.
          </p>
        </div>
      </div>
      <div className="card">
        <div className="filters">
          <select value={status} onChange={(e) => setStatus(e.target.value as SlotStatus)}>
            {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
          <select value={setting} onChange={(e) => setSetting(e.target.value as CareSetting | '')}>
            <option value="">All settings</option>
            <option value="INPATIENT">Inpatient</option>
            <option value="OUTPATIENT">Outpatient</option>
          </select>
          <button className="secondary small" onClick={reload}>Refresh</button>
        </div>
        <ErrorBox message={error ?? actionError} />
        <Loading loading={loading} />
        {data && (data.length === 0 ? <div className="empty">Nothing here.</div> : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Patient</th><th>Drug and dose</th><th>Setting</th><th>Due</th><th>Window closes</th><th>Status</th>{canRecord && status === 'PENDING' && <th />}</tr>
              </thead>
              <tbody>
                {data.map((s) => {
                  const left = minutesLeft(s.windowClosesAt);
                  return (
                    <tr key={s.id}>
                      <td><Link to={`/patients/${s.patientId}`}>{s.patientName}</Link></td>
                      <td>{label(s.drugName)}<div className="muted"><small>{s.doseAmount} {s.doseUnit} {label(s.formulation).toLowerCase()}</small></div></td>
                      <td><StatusBadge status={s.careSetting} /></td>
                      <td>{fmtDateTime(s.dueAt)}</td>
                      <td>
                        {fmtDateTime(s.windowClosesAt)}
                        {s.status === 'PENDING' && <div className={left <= 10 ? 'badge red' : 'muted'}><small>{left > 0 ? `${left} min left` : 'closing'}</small></div>}
                      </td>
                      <td><StatusBadge status={s.status} /></td>
                      {canRecord && status === 'PENDING' && (
                        <td>
                          <div className="actions" style={{ marginTop: 0 }}>
                            <button className="small" disabled={busyId === s.id} onClick={() => record(s, 'ADMINISTERED')}>Given</button>
                            <button className="secondary small" disabled={busyId === s.id} onClick={() => record(s, 'REFUSED')}>Refused</button>
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </div>
  );
}
