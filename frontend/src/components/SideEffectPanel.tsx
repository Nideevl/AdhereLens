import { useState, type FormEvent } from 'react';
import { get, post } from '../api';
import { ErrorBox, Field, Modal, StatusBadge } from './ui';
import { fmtDateTime, label } from '../format';
import { useLoad } from '../hooks';
import type { Patient, Regimen, SideEffectReport } from '../types';

interface Props {
  patient: Patient;
  regimens: Regimen[];
  canReport: boolean;
  canRetract: boolean;
}

export default function SideEffectPanel({ patient, regimens, canReport, canRetract }: Props) {
  const reports = useLoad(() => get<SideEffectReport[]>(`/api/patients/${patient.id}/side-effects`), [patient.id]);
  const catalog = useLoad(() => get<string[]>('/api/side-effects/catalog'), []);
  const [reporting, setReporting] = useState(false);
  const [regimenId, setRegimenId] = useState('');
  const [picked, setPicked] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [retracting, setRetracting] = useState<SideEffectReport | null>(null);

  const regimenName = (id: string) => {
    const r = regimens.find((x) => x.id === id);
    return r ? label(r.drugName) : '-';
  };

  function toggle(code: string) {
    setPicked((p) => (p.includes(code) ? p.filter((c) => c !== code) : [...p, code]));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!regimenId) { setError('Choose the regimen the side effect belongs to'); return; }
    if (picked.length === 0) { setError('Pick at least one side effect'); return; }
    setBusy(true);
    try {
      await post(`/api/regimens/${regimenId}/side-effects`, { sideEffects: picked });
      setPicked([]);
      setReporting(false);
      reports.reload();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card">
      <div className="page-head" style={{ marginBottom: 8 }}>
        <h2>Side effects</h2>
        {canReport && regimens.length > 0 && (
          <button onClick={() => { setReporting(!reporting); setRegimenId(regimens[0].id); }}>{reporting ? 'Cancel' : 'Report side effect'}</button>
        )}
      </div>

      {reporting && (
        <form className="reg" onSubmit={submit}>
          <Field label="Regimen">
            <select value={regimenId} onChange={(e) => setRegimenId(e.target.value)}>
              {regimens.map((r) => <option key={r.id} value={r.id}>{label(r.drugName)} ({r.status.toLowerCase()})</option>)}
            </select>
          </Field>
          <Field label="What was observed" hint="Choose from the fixed list. Use Other only if nothing fits.">
            <div className="checks">
              {(catalog.data ?? []).map((c) => (
                <label key={c}><input type="checkbox" checked={picked.includes(c)} onChange={() => toggle(c)} /> {label(c)}</label>
              ))}
            </div>
          </Field>
          <ErrorBox message={error} />
          <div className="actions"><button disabled={busy}>{busy ? 'Saving...' : 'Save report'}</button></div>
        </form>
      )}

      <ErrorBox message={reports.error} />
      {reports.data && (reports.data.length === 0 ? <div className="empty">No side effects reported.</div> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Side effect</th><th>Regimen</th><th>Reported</th><th>By</th><th>Status</th><th /></tr></thead>
            <tbody>
              {reports.data.map((s) => (
                <tr key={s.id}>
                  <td>{label(s.sideEffect)}</td>
                  <td>{regimenName(s.regimenId)}</td>
                  <td>{fmtDateTime(s.reportedAt)}</td>
                  <td>{s.reportedByName ?? '-'} <span className="muted">({label(s.reportedByRole)})</span></td>
                  <td>
                    <StatusBadge status={s.status} />
                    {s.retractReason && <div className="muted"><small>{s.retractReason}</small></div>}
                  </td>
                  <td>{canRetract && s.status === 'ACTIVE' && <button className="secondary small" onClick={() => setRetracting(s)}>Retract</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}

      {retracting && (
        <RetractModal report={retracting} onClose={() => setRetracting(null)} onDone={() => { setRetracting(null); reports.reload(); }} />
      )}
    </div>
  );
}

function RetractModal({ report, onClose, onDone }: { report: SideEffectReport; onClose: () => void; onDone: () => void }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await post(`/api/side-effects/${report.id}/retract`, { reason: reason.trim() });
      onDone();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={`Retract: ${label(report.sideEffect)}`} onClose={onClose}>
      <form onSubmit={submit}>
        <p className="muted">The entry stays on record as retracted. It is not deleted.</p>
        <Field label="Reason"><input value={reason} onChange={(e) => setReason(e.target.value)} required maxLength={200} /></Field>
        <ErrorBox message={error} />
        <div className="actions">
          <button className="danger" disabled={busy}>{busy ? 'Saving...' : 'Retract'}</button>
          <button type="button" className="secondary" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}
