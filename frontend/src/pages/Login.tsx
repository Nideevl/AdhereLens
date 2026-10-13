import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { ErrorBox, Field } from '../components/ui';

// Local development accounts from the handoff notes. Remove before any real deployment.
const DEMO = [
  { who: 'Platform admin', email: 'admin@adherelens.local', password: 'ChangeMe123!' },
  { who: 'Hospital A admin', email: 'admin@hospa.test', password: 'password123' },
  { who: 'Doctor', email: 'doc1@hospa.test', password: 'password123' },
  { who: 'Nurse', email: 'nurse1@hospa.test', password: 'password123' },
];

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(email.trim(), password);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card narrow">
      <h1>AdhereLens</h1>
      <p className="muted">Log in with the account your hospital gave you.</p>
      <form onSubmit={submit} className="grid" style={{ gridTemplateColumns: '1fr' }}>
        <Field label="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </Field>
        <Field label="Password">
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        <ErrorBox message={error} />
        <button disabled={busy}>{busy ? 'Logging in...' : 'Log in'}</button>
      </form>
      <div className="hint-box">
        <div>
          Accounts are created by your administrator: the platform admin registers a hospital, and the hospital
          admin adds doctors and nurses.
        </div>
        <div style={{ marginTop: 10 }}>Dev logins (click to fill):</div>
        <div className="actions" style={{ marginTop: 6 }}>
          {DEMO.map((d) => (
            <button key={d.email} type="button" className="secondary small" onClick={() => { setEmail(d.email); setPassword(d.password); }}>
              {d.who}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
