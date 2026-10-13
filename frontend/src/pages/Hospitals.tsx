import { useState, type FormEvent } from 'react';
import { get, post } from '../api';
import { ErrorBox, Field, Loading, StatusBadge } from '../components/ui';
import { toCode } from '../format';
import { useLoad } from '../hooks';
import type { CreateHospitalInput, CreateHospitalResult, Hospital } from '../types';

const EMPTY: CreateHospitalInput = { code: '', name: '', adminEmail: '', adminFullName: '', adminPassword: '' };

export default function Hospitals() {
  const { data, error, loading, reload } = useLoad(() => get<Hospital[]>('/api/hospitals'), []);
  const [form, setForm] = useState<CreateHospitalInput>(EMPTY);
  const [open, setOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [created, setCreated] = useState<CreateHospitalResult | null>(null);
  const [busy, setBusy] = useState(false);

  const set = (k: keyof CreateHospitalInput, v: string) => setForm((f) => ({ ...f, [k]: v }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setFormError(null);
    try {
      const result = await post<CreateHospitalResult>('/api/hospitals', form);
      setCreated(result);
      setForm(EMPTY);
      setOpen(false);
      reload();
    } catch (err) {
      setFormError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Hospitals</h1>
          <p className="muted">Register a hospital and its first administrator. The administrator then adds doctors and nurses.</p>
        </div>
        <button onClick={() => { setOpen(!open); setCreated(null); }}>{open ? 'Cancel' : 'Register hospital'}</button>
      </div>

      {created && (
        <div className="success">
          <strong>{created.name}</strong> registered. The API key below is shown only once, so copy it now:
          <div><code>{created.apiKey}</code></div>
        </div>
      )}

      {open && (
        <form className="card" onSubmit={submit}>
          <h2>Register hospital</h2>
          <div className="grid">
            <Field label="Hospital code" hint="Upper-case letters, digits, underscore">
              <input value={form.code} onChange={(e) => set('code', toCode(e.target.value))} required maxLength={50} />
            </Field>
            <Field label="Hospital name"><input value={form.name} onChange={(e) => set('name', e.target.value)} required /></Field>
            <Field label="Admin full name"><input value={form.adminFullName} onChange={(e) => set('adminFullName', e.target.value)} required /></Field>
            <Field label="Admin email"><input type="email" value={form.adminEmail} onChange={(e) => set('adminEmail', e.target.value)} required /></Field>
            <Field label="Admin password" hint="8 to 72 characters">
              <input type="password" value={form.adminPassword} onChange={(e) => set('adminPassword', e.target.value)} required minLength={8} maxLength={72} />
            </Field>
          </div>
          <ErrorBox message={formError} />
          <div className="actions"><button disabled={busy}>{busy ? 'Saving...' : 'Register'}</button></div>
        </form>
      )}

      <div className="card">
        <ErrorBox message={error} />
        <Loading loading={loading} />
        {data && (data.length === 0 ? <div className="empty">No hospitals yet.</div> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Code</th><th>Name</th><th>Status</th></tr></thead>
              <tbody>
                {data.map((h) => (
                  <tr key={h.id}><td>{h.code}</td><td>{h.name}</td><td><StatusBadge status={h.status === 'ACTIVE' ? 'ACTIVE' : 'DISCONTINUED'} /></td></tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </div>
  );
}
