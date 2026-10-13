import { useState, type FormEvent } from 'react';
import { get, post } from '../api';
import { ErrorBox, Field, Loading, StatusBadge } from '../components/ui';
import { label, textOrNull } from '../format';
import { useLoad } from '../hooks';
import type { Role, Staff as StaffMember, StaffInput } from '../types';

const EMPTY = { email: '', fullName: '', password: '', role: 'NURSE' as Role, specialty: '' };

export default function Staff() {
  const { data, error, loading, reload } = useLoad(() => get<StaffMember[]>('/api/staff'), []);
  const [form, setForm] = useState(EMPTY);
  const [open, setOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setFormError(null);
    const body: StaffInput = {
      email: form.email.trim(), fullName: form.fullName.trim(), password: form.password,
      role: form.role, specialty: form.role === 'DOCTOR' ? textOrNull(form.specialty) : null,
    };
    try {
      await post('/api/staff', body);
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
          <h1>Staff</h1>
          <p className="muted">Doctors and nurses of your hospital. Only you can add them.</p>
        </div>
        <button onClick={() => setOpen(!open)}>{open ? 'Cancel' : 'Add staff'}</button>
      </div>

      {open && (
        <form className="card" onSubmit={submit}>
          <h2>Add staff member</h2>
          <div className="grid">
            <Field label="Role">
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
                <option value="NURSE">Nurse</option>
                <option value="DOCTOR">Doctor</option>
              </select>
            </Field>
            <Field label="Full name"><input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required /></Field>
            <Field label="Email"><input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></Field>
            <Field label="Password" hint="8 to 72 characters">
              <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} maxLength={72} />
            </Field>
            {form.role === 'DOCTOR' && (
              <Field label="Specialty"><input value={form.specialty} onChange={(e) => setForm({ ...form, specialty: e.target.value })} maxLength={100} /></Field>
            )}
          </div>
          <ErrorBox message={formError} />
          <div className="actions"><button disabled={busy}>{busy ? 'Saving...' : 'Add'}</button></div>
        </form>
      )}

      <div className="card">
        <ErrorBox message={error} />
        <Loading loading={loading} />
        {data && (data.length === 0 ? <div className="empty">No staff yet.</div> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Specialty</th><th>Status</th></tr></thead>
              <tbody>
                {data.map((s) => (
                  <tr key={s.id}>
                    <td>{s.fullName}</td><td>{s.email}</td><td>{label(s.role)}</td><td>{s.specialty ?? '-'}</td>
                    <td><StatusBadge status={s.active ? 'ACTIVE' : 'DISCONTINUED'} /></td>
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
