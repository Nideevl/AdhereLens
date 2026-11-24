import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { drugLabel, fmtDateTime, label } from '../format';
import type { DoseUnit, Formulation } from '../types';

/** What the no-login link page gets back. The signed token in the URL is the only credential. */
interface PatientDoseView {
  drugName: string; formulation: Formulation; doseAmount: number; doseUnit: DoseUnit;
  dueAt: string; windowClosesAt: string;
  status: 'PENDING' | 'HIT' | 'DELAYED' | 'MISSED' | 'REFUSED' | 'CANCELLED';
  canRecord: boolean;
}

type Outcome = 'ADMINISTERED' | 'REFUSED';

// Plain fetch on purpose: this page must never send a staff login token, and a 401/410 here
// must not trigger the staff "logged out" handler in api.ts.
async function call(method: string, token: string, path = '', body?: unknown): Promise<PatientDoseView> {
  const res = await fetch(`/api/public/dose/${encodeURIComponent(token)}${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.ok) return res.json();
  let message = 'Something went wrong. Please try again.';
  try { message = (await res.json()).error ?? message; } catch { /* keep default */ }
  throw Object.assign(new Error(message), { status: res.status });
}

function done(status: PatientDoseView['status']): string | null {
  switch (status) {
    case 'HIT': case 'DELAYED': return 'Thank you. Your dose is recorded as taken.';
    case 'REFUSED': return 'Your answer is recorded: not taken. Your care team will see it.';
    case 'MISSED': return 'The time for this dose has passed.';
    case 'CANCELLED': return 'This dose was cancelled by your doctor.';
    default: return null;
  }
}

export default function PatientDose() {
  const { token = '' } = useParams();
  const [dose, setDose] = useState<PatientDoseView | null>(null);
  const [error, setError] = useState<{ text: string; status: number } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    call('GET', token).then(setDose).catch((e) => setError({ text: e.message, status: e.status }));
  }, [token]);

  async function record(outcome: Outcome) {
    setBusy(true);
    try {
      setDose(await call('POST', token, '/record', { outcome }));
      setError(null);
    } catch (e) {
      const err = e as Error & { status: number };
      setError({ text: err.message, status: err.status });
    } finally {
      setBusy(false);
    }
  }

  if (error && !dose) {
    const expired = error.status === 410;
    return (
      <div className="card narrow">
        <h2>{expired ? 'This link has expired' : 'This link is not valid'}</h2>
        <p>{expired
          ? 'The time for this dose has passed. Please take your next dose as prescribed, or ask your care team.'
          : 'Please use the link from your latest reminder message, or ask your care team.'}</p>
      </div>
    );
  }
  if (!dose) return <div className="card narrow"><p>Loading...</p></div>;

  const finished = done(dose.status);
  return (
    <div className="card narrow">
      <h2>Your medicine dose</h2>
      <p><strong>{drugLabel(dose.drugName)}</strong><br />
        {dose.doseAmount} {dose.doseUnit.toLowerCase()} · {label(dose.formulation)}</p>
      <p>Due at {fmtDateTime(dose.dueAt)}<br />You can confirm until {fmtDateTime(dose.windowClosesAt)}</p>

      {finished && <p>{finished}</p>}
      {!finished && !dose.canRecord && <p>This dose is not open yet. Please use this link at the dose time.</p>}
      {error && <p style={{ color: 'var(--red)' }}>{error.text}</p>}

      {dose.canRecord && (
        <div style={{ display: 'flex', gap: 10 }}>
          <button disabled={busy} onClick={() => record('ADMINISTERED')}>I took it</button>
          <button className="secondary" disabled={busy} onClick={() => record('REFUSED')}>I did not take it</button>
        </div>
      )}
    </div>
  );
}
