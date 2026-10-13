import { useState, type FormEvent } from 'react';
import { ErrorBox, Field } from './ui';
import { numOrNull, textOrNull, toCode } from '../format';
import { CARE_SETTINGS, GENDERS, type CareSetting, type Gender, type Patient, type PatientInput } from '../types';

// Suggestions only. The backend accepts any coded value (upper-case letters, digits, underscore).
const CONDITIONS = ['TYPE2_DIABETES', 'TYPE1_DIABETES', 'HYPERTENSION', 'ASTHMA', 'EPILEPSY', 'HEART_FAILURE', 'TUBERCULOSIS', 'HYPOTHYROIDISM'];

interface Props {
  initial?: Patient;
  submitText: string;
  onSubmit: (input: PatientInput) => Promise<void>;
  onCancel: () => void;
}

export default function PatientForm({ initial, submitText, onSubmit, onCancel }: Props) {
  const [fullName, setFullName] = useState(initial?.fullName ?? '');
  const [dob, setDob] = useState(initial?.dateOfBirth ?? '');
  const [gender, setGender] = useState<Gender>(initial?.gender ?? 'FEMALE');
  const [careSetting, setCareSetting] = useState<CareSetting>(initial?.careSetting ?? 'OUTPATIENT');
  const [condition, setCondition] = useState(initial?.conditionCode ?? '');
  const [phone, setPhone] = useState(initial?.phone ?? '');
  const [guardian, setGuardian] = useState(initial?.guardianPhone ?? '');
  const [weight, setWeight] = useState(initial?.weightKg != null ? String(initial.weightKg) : '');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (careSetting === 'OUTPATIENT' && !phone.trim() && !guardian.trim()) {
      setError('An outpatient needs a phone or a guardian phone so reminders can reach them');
      return;
    }
    setBusy(true);
    try {
      await onSubmit({
        fullName: fullName.trim(), phone: textOrNull(phone), guardianPhone: textOrNull(guardian),
        dateOfBirth: dob, gender, careSetting, conditionCode: condition, weightKg: numOrNull(weight),
      });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <div className="grid">
        <Field label="Full name"><input value={fullName} onChange={(e) => setFullName(e.target.value)} required maxLength={200} /></Field>
        <Field label="Date of birth"><input type="date" value={dob} onChange={(e) => setDob(e.target.value)} required max={new Date().toISOString().slice(0, 10)} /></Field>
        <Field label="Gender">
          <select value={gender} onChange={(e) => setGender(e.target.value as Gender)}>
            {GENDERS.map((g) => <option key={g} value={g}>{g.charAt(0) + g.slice(1).toLowerCase()}</option>)}
          </select>
        </Field>
        <Field label="Care setting">
          <select value={careSetting} onChange={(e) => setCareSetting(e.target.value as CareSetting)}>
            {CARE_SETTINGS.map((c) => <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>)}
          </select>
        </Field>
        <Field label="Condition code" hint="Coded value, no free text">
          <input list="conditions" value={condition} onChange={(e) => setCondition(toCode(e.target.value))} required maxLength={50} />
          <datalist id="conditions">{CONDITIONS.map((c) => <option key={c} value={c} />)}</datalist>
        </Field>
        <Field label="Weight (kg)" hint="Optional">
          <input type="number" step="0.01" min="0.3" max="500" value={weight} onChange={(e) => setWeight(e.target.value)} />
        </Field>
        <Field label="Phone" hint={careSetting === 'OUTPATIENT' ? 'Phone or guardian phone required' : 'Optional'}>
          <input value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={30} />
        </Field>
        <Field label="Guardian phone"><input value={guardian} onChange={(e) => setGuardian(e.target.value)} maxLength={30} /></Field>
      </div>
      <ErrorBox message={error} />
      <div className="actions">
        <button disabled={busy}>{busy ? 'Saving...' : submitText}</button>
        <button type="button" className="secondary" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
