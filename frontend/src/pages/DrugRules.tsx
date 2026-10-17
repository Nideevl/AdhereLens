import { useState, type FormEvent } from 'react';
import { get, post } from '../api';
import { useAuth } from '../auth';
import { ErrorBox, Field, Loading, TimesInput } from '../components/ui';
import { fmtTime, label, numOrNull, toCode, drugLabel } from '../format';
import { useLoad } from '../hooks';
import { DOSE_UNITS, FORMULATIONS, GENDERS, type DoseUnit, type DrugRule, type DrugRuleInput, type Formulation, type Gender } from '../types';

export default function DrugRules() {
  const { auth } = useAuth();
  const canEdit = auth?.role === 'DOCTOR';
  const [filter, setFilter] = useState('');
  const [adding, setAdding] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const { data, error, loading, reload } = useLoad(
    () => get<DrugRule[]>(`/api/drug-rules${filter ? `?drugName=${encodeURIComponent(filter)}` : ''}`),
    [filter],
  );

  async function deactivate(rule: DrugRule) {
    if (!window.confirm(`Deactivate this rule for ${drugLabel(rule.drugName)}?`)) return;
    setActionError(null);
    try {
      await post(`/api/drug-rules/${rule.id}/deactivate`);
      reload();
    } catch (err) {
      setActionError((err as Error).message);
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Drug rules</h1>
          <p className="muted">Your hospital's own dosing rules. They are only used to suggest a dose when prescribing; the doctor always confirms.</p>
        </div>
        {canEdit && <button onClick={() => setAdding(!adding)}>{adding ? 'Cancel' : 'Add rule'}</button>}
      </div>

      {adding && <RuleForm onDone={() => { setAdding(false); reload(); }} />}

      <div className="card">
        <div className="filters">
          <input placeholder="Filter by drug code" value={filter} onChange={(e) => setFilter(toCode(e.target.value))} />
        </div>
        <ErrorBox message={error ?? actionError} />
        <Loading loading={loading} />
        {data && (data.length === 0 ? <div className="empty">No rules.</div> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Drug</th><th>Applies to</th><th>Suggested dose</th><th>Times</th>{canEdit && <th />}</tr></thead>
              <tbody>
                {data.map((r) => (
                  <tr key={r.id}>
                    <td>{drugLabel(r.drugName)}<div className="muted"><small>{label(r.formulation)}</small></div></td>
                    <td>
                      Age {r.minAgeYears} to {r.maxAgeYears}
                      {r.gender && `, ${label(r.gender).toLowerCase()}`}
                      {(r.minWeightKg != null || r.maxWeightKg != null) && (
                        <div className="muted"><small>Weight {r.minWeightKg ?? 'any'} to {r.maxWeightKg ?? 'any'} kg</small></div>
                      )}
                    </td>
                    <td>
                      {r.doseAmount} {r.doseUnit}
                      {r.strengthPerUnit != null && <div className="muted"><small>{r.strengthPerUnit} {r.doseUnit} per unit</small></div>}
                    </td>
                    <td>{r.doseTimes.map(fmtTime).join(', ')}</td>
                    {canEdit && <td><button className="secondary small" onClick={() => deactivate(r)}>Deactivate</button></td>}
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

function RuleForm({ onDone }: { onDone: () => void }) {
  const [drug, setDrug] = useState('');
  const [formulation, setFormulation] = useState<Formulation>('TABLET');
  const [minAge, setMinAge] = useState('18');
  const [maxAge, setMaxAge] = useState('65');
  const [gender, setGender] = useState<Gender | ''>('');
  const [minWeight, setMinWeight] = useState('');
  const [maxWeight, setMaxWeight] = useState('');
  const [amount, setAmount] = useState('');
  const [unit, setUnit] = useState<DoseUnit>('MG');
  const [strength, setStrength] = useState('');
  const [times, setTimes] = useState<string[]>(['08:00']);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    const body: DrugRuleInput = {
      drugName: drug, formulation, minAgeYears: Number(minAge), maxAgeYears: Number(maxAge),
      gender: gender || null, minWeightKg: numOrNull(minWeight), maxWeightKg: numOrNull(maxWeight),
      doseAmount: Number(amount), doseUnit: unit, strengthPerUnit: numOrNull(strength), doseTimes: times,
    };
    try {
      await post('/api/drug-rules', body);
      onDone();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>New rule</h2>
      <div className="grid">
        <Field label="Drug code" hint="Coded value, e.g. METFORMIN"><input value={drug} onChange={(e) => setDrug(toCode(e.target.value))} required maxLength={100} /></Field>
        <Field label="Formulation">
          <select value={formulation} onChange={(e) => setFormulation(e.target.value as Formulation)}>
            {FORMULATIONS.map((f) => <option key={f} value={f}>{label(f)}</option>)}
          </select>
        </Field>
        <Field label="Min age (years)"><input type="number" min="0" max="150" value={minAge} onChange={(e) => setMinAge(e.target.value)} required /></Field>
        <Field label="Max age (years)"><input type="number" min="0" max="150" value={maxAge} onChange={(e) => setMaxAge(e.target.value)} required /></Field>
        <Field label="Gender" hint="Optional">
          <select value={gender} onChange={(e) => setGender(e.target.value as Gender | '')}>
            <option value="">Any</option>
            {GENDERS.map((g) => <option key={g} value={g}>{label(g)}</option>)}
          </select>
        </Field>
        <Field label="Min weight (kg)" hint="Optional. Needs the patient's weight"><input type="number" step="0.01" min="0.3" max="500" value={minWeight} onChange={(e) => setMinWeight(e.target.value)} /></Field>
        <Field label="Max weight (kg)" hint="Optional"><input type="number" step="0.01" min="0.3" max="500" value={maxWeight} onChange={(e) => setMaxWeight(e.target.value)} /></Field>
        <Field label="Suggested dose"><input type="number" step="any" min="0.001" value={amount} onChange={(e) => setAmount(e.target.value)} required /></Field>
        <Field label="Unit">
          <select value={unit} onChange={(e) => setUnit(e.target.value as DoseUnit)}>
            {DOSE_UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
          </select>
        </Field>
        <Field label="Strength per tablet or ml" hint="Optional, same unit. Used to show how many tablets or ml">
          <input type="number" step="any" min="0.001" value={strength} onChange={(e) => setStrength(e.target.value)} />
        </Field>
      </div>
      <Field label="Dose times (daily)"><TimesInput value={times} onChange={setTimes} /></Field>
      <ErrorBox message={error} />
      <div className="actions"><button disabled={busy}>{busy ? 'Saving...' : 'Save rule'}</button></div>
    </form>
  );
}
