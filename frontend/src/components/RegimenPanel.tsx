import { useState, type FormEvent } from 'react';
import { get, patch, post } from '../api';
import { ErrorBox, Field, Modal, StatusBadge, TimesInput } from './ui';
import { fmtDate, fmtDateTime, fmtTime, label, numOrNull, textOrNull, toCode, todayIso, drugLabel } from '../format';
import { useLoad } from '../hooks';
import {
  DOSE_UNITS, FORMULATIONS,
  type DoseUnit, type Formulation, type Patient, type Regimen, type RegimenChange, type RegimenInput,
  type RegimenUpdate, type Suggestion, type SuggestionResponse,
} from '../types';

interface Props {
  patient: Patient;
  regimens: Regimen[];
  canPrescribe: boolean;
  canEdit: boolean;
  canDiscontinue: boolean;
  onChanged: () => void;
}

export default function RegimenPanel({ patient, regimens, canPrescribe, canEdit, canDiscontinue, onChanged }: Props) {
  const [prescribing, setPrescribing] = useState(false);
  const [editing, setEditing] = useState<Regimen | null>(null);
  const [history, setHistory] = useState<Regimen | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function discontinue(r: Regimen) {
    if (!window.confirm(`Discontinue ${drugLabel(r.drugName)}? Future doses will be cancelled.`)) return;
    setActionError(null);
    try {
      await post(`/api/regimens/${r.id}/discontinue`);
      onChanged();
    } catch (err) {
      setActionError((err as Error).message);
    }
  }

  return (
    <div className="card">
      <div className="page-head" style={{ marginBottom: 8 }}>
        <h2>Regimens</h2>
        {canPrescribe && patient.active && (
          <button onClick={() => setPrescribing(!prescribing)}>{prescribing ? 'Cancel' : 'Prescribe'}</button>
        )}
      </div>
      <ErrorBox message={actionError} />
      {prescribing && (
        <PrescribeForm patient={patient} onDone={() => { setPrescribing(false); onChanged(); }} />
      )}
      {regimens.length === 0 && <div className="empty">No regimens yet.</div>}
      {regimens.map((r) => (
        <div className="reg" key={r.id}>
          <div className="reg-head">
            <strong>{drugLabel(r.drugName)}</strong>
            <StatusBadge status={r.status} />
          </div>
          <div className="kv">
            <span>{r.doseAmount} {r.doseUnit} {label(r.formulation).toLowerCase()}</span>
            <span>At {r.doseTimes.map(fmtTime).join(', ')}</span>
            <span>Grace window {r.graceWindowMinutes} min</span>
            <span>{fmtDate(r.startDate)} to {r.endDate ? fmtDate(r.endDate) : 'no end date'}</span>
            {r.discontinuedOn && <span>Discontinued {fmtDate(r.discontinuedOn)}</span>}
          </div>
          <div className="actions">
            <button className="secondary small" onClick={() => setHistory(r)}>History</button>
            {canEdit && r.status === 'ACTIVE' && <button className="secondary small" onClick={() => setEditing(r)}>Edit</button>}
            {canDiscontinue && r.status === 'ACTIVE' && <button className="danger small" onClick={() => discontinue(r)}>Discontinue</button>}
          </div>
        </div>
      ))}
      {editing && <EditModal regimen={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); onChanged(); }} />}
      {history && <HistoryModal regimen={history} onClose={() => setHistory(null)} />}
    </div>
  );
}

function PrescribeForm({ patient, onDone }: { patient: Patient; onDone: () => void }) {
  const [drug, setDrug] = useState('');
  const [formulation, setFormulation] = useState<Formulation>('TABLET');
  const [amount, setAmount] = useState('');
  const [unit, setUnit] = useState<DoseUnit>('MG');
  const [times, setTimes] = useState<string[]>(['08:00']);
  const [startDate, setStartDate] = useState(todayIso());
  const [endDate, setEndDate] = useState('');
  const [grace, setGrace] = useState('60');
  const [suggestion, setSuggestion] = useState<SuggestionResponse | null>(null);
  const [suggestError, setSuggestError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function suggest() {
    setSuggestError(null);
    setSuggestion(null);
    if (!drug) { setSuggestError('Enter the drug code first'); return; }
    try {
      setSuggestion(await get<SuggestionResponse>(`/api/patients/${patient.id}/dose-suggestions?drugName=${encodeURIComponent(drug)}`));
    } catch (err) {
      setSuggestError((err as Error).message);
    }
  }

  function use(s: Suggestion) {
    setFormulation(s.formulation);
    setAmount(String(s.doseAmount));
    setUnit(s.doseUnit);
    setTimes(s.doseTimes.map(fmtTime));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    const body: RegimenInput = {
      drugName: drug, formulation, doseAmount: Number(amount), doseUnit: unit, doseTimes: times,
      startDate, endDate: textOrNull(endDate), graceWindowMinutes: numOrNull(grace) ?? 60,
    };
    try {
      await post(`/api/patients/${patient.id}/regimens`, body);
      onDone();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="reg" onSubmit={submit}>
      <div className="grid">
        <Field label="Drug code" hint="Coded value, e.g. METFORMIN">
          <input value={drug} onChange={(e) => setDrug(toCode(e.target.value))} required maxLength={100} />
        </Field>
        <Field label="Formulation">
          <select value={formulation} onChange={(e) => setFormulation(e.target.value as Formulation)}>
            {FORMULATIONS.map((f) => <option key={f} value={f}>{label(f)}</option>)}
          </select>
        </Field>
        <Field label="Dose amount"><input type="number" step="any" min="0.001" value={amount} onChange={(e) => setAmount(e.target.value)} required /></Field>
        <Field label="Unit">
          <select value={unit} onChange={(e) => setUnit(e.target.value as DoseUnit)}>
            {DOSE_UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
          </select>
        </Field>
        <Field label="Start date"><input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required /></Field>
        <Field label="End date" hint="Optional"><input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} /></Field>
        <Field label="Grace window (min)" hint="5 to 240"><input type="number" min="5" max="240" value={grace} onChange={(e) => setGrace(e.target.value)} /></Field>
      </div>
      <Field label="Dose times (daily)"><TimesInput value={times} onChange={setTimes} /></Field>

      <div className="actions">
        <button type="button" className="secondary" onClick={suggest}>Suggest from hospital rules</button>
      </div>
      <ErrorBox message={suggestError} />
      {suggestion && (
        <div className="suggest">
          <div className="muted">
            For this patient ({suggestion.ageYears} years, {suggestion.weightKg != null ? `${suggestion.weightKg} kg` : 'weight not recorded'}).
            A suggestion only: you confirm or change it before saving.
          </div>
          {suggestion.suggestions.map((s) => (
            <div key={s.ruleId} className="reg-head" style={{ marginTop: 8 }}>
              <span>
                <strong>{s.doseAmount} {s.doseUnit}</strong> {label(s.formulation).toLowerCase()} at {s.doseTimes.map(fmtTime).join(', ')}
                {s.unitsToAdminister != null && ` (${s.unitsToAdminister} ${s.unitsLabel})`}
              </span>
              <button type="button" className="secondary small" onClick={() => use(s)}>Use this</button>
            </div>
          ))}
          {suggestion.notes.map((n, i) => <div className="note" key={i}>{n}</div>)}
        </div>
      )}

      <ErrorBox message={error} />
      <div className="actions"><button disabled={busy}>{busy ? 'Saving...' : 'Save regimen'}</button></div>
    </form>
  );
}

function EditModal({ regimen, onClose, onSaved }: { regimen: Regimen; onClose: () => void; onSaved: () => void }) {
  const [amount, setAmount] = useState(String(regimen.doseAmount));
  const [unit, setUnit] = useState<DoseUnit>(regimen.doseUnit);
  const [times, setTimes] = useState(regimen.doseTimes.map(fmtTime));
  const [endDate, setEndDate] = useState(regimen.endDate ?? '');
  const [grace, setGrace] = useState(String(regimen.graceWindowMinutes));
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const update: RegimenUpdate = {};
    if (Number(amount) !== regimen.doseAmount) update.doseAmount = Number(amount);
    if (unit !== regimen.doseUnit) update.doseUnit = unit;
    if (JSON.stringify([...times].sort()) !== JSON.stringify(regimen.doseTimes.map(fmtTime).sort())) update.doseTimes = times;
    if (endDate && endDate !== (regimen.endDate ?? '')) update.endDate = endDate;
    if (Number(grace) !== regimen.graceWindowMinutes) update.graceWindowMinutes = Number(grace);
    if (Object.keys(update).length === 0) { setError('Nothing changed'); return; }
    if (reason.trim()) update.reason = reason.trim();
    setBusy(true);
    try {
      await patch(`/api/regimens/${regimen.id}`, update);
      onSaved();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={`Edit ${drugLabel(regimen.drugName)}`} onClose={onClose}>
      <form onSubmit={submit}>
        <div className="grid">
          <Field label="Dose amount"><input type="number" step="any" min="0.001" value={amount} onChange={(e) => setAmount(e.target.value)} required /></Field>
          <Field label="Unit">
            <select value={unit} onChange={(e) => setUnit(e.target.value as DoseUnit)}>
              {DOSE_UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
            </select>
          </Field>
          <Field label="End date"><input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} /></Field>
          <Field label="Grace window (min)"><input type="number" min="5" max="240" value={grace} onChange={(e) => setGrace(e.target.value)} /></Field>
        </div>
        <Field label="Dose times"><TimesInput value={times} onChange={setTimes} /></Field>
        <Field label="Reason" hint="Saved in the change history">
          <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={200} />
        </Field>
        <ErrorBox message={error} />
        <div className="actions">
          <button disabled={busy}>{busy ? 'Saving...' : 'Save changes'}</button>
          <button type="button" className="secondary" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}

function HistoryModal({ regimen, onClose }: { regimen: Regimen; onClose: () => void }) {
  const { data, error, loading } = useLoad(() => get<RegimenChange[]>(`/api/regimens/${regimen.id}/history`), [regimen.id]);
  return (
    <Modal title={`History: ${drugLabel(regimen.drugName)}`} onClose={onClose}>
      <ErrorBox message={error} />
      {loading && <p className="muted">Loading...</p>}
      {data && (data.length === 0 ? <div className="empty">No changes recorded.</div> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>When</th><th>Who</th><th>Field</th><th>Change</th><th>Reason</th></tr></thead>
            <tbody>
              {data.map((c, i) => (
                <tr key={`${c.changeId}-${i}`}>
                  <td>{fmtDateTime(c.changedAt)}</td>
                  <td>{c.changedByName ?? '-'} <span className="muted">({label(c.changedByRole)})</span></td>
                  <td>{label(c.field)}</td>
                  <td>{c.oldValue ?? '-'} to {c.newValue ?? '-'}</td>
                  <td>{c.reason ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </Modal>
  );
}
