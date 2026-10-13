import type { ReactNode } from 'react';

export function ErrorBox({ message }: { message: string | null }) {
  if (!message) return null;
  return <div className="error">{message}</div>;
}

export function Loading({ loading }: { loading: boolean }) {
  return loading ? <p className="muted">Loading...</p> : null;
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small className="muted">{hint}</small>}
    </label>
  );
}

export function Badge({ text, tone }: { text: string; tone?: string }) {
  return <span className={`badge ${tone ?? ''}`}>{text}</span>;
}

const STATUS_TONE: Record<string, string> = {
  PENDING: 'blue', HIT: 'green', DELAYED: 'amber', MISSED: 'red', REFUSED: 'gray', CANCELLED: 'gray',
  ACTIVE: 'green', COMPLETED: 'gray', DISCONTINUED: 'red', OPEN: 'amber', RESOLVED: 'green',
  RETRACTED: 'gray', INPATIENT: 'blue', OUTPATIENT: 'teal',
};

export function StatusBadge({ status }: { status: string }) {
  return <Badge text={status.charAt(0) + status.slice(1).toLowerCase()} tone={STATUS_TONE[status]} />;
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="link" onClick={onClose}>Close</button>
        </div>
        {children}
      </div>
    </div>
  );
}

/** One or more HH:mm time inputs (the backend allows 1 to 6 dose times a day). */
export function TimesInput({ value, onChange }: { value: string[]; onChange: (v: string[]) => void }) {
  return (
    <div className="times">
      {value.map((t, i) => (
        <span key={i} className="time-chip">
          <input
            type="time"
            value={t}
            required
            onChange={(e) => onChange(value.map((x, j) => (j === i ? e.target.value : x)))}
          />
          {value.length > 1 && (
            <button type="button" className="link" onClick={() => onChange(value.filter((_, j) => j !== i))}>x</button>
          )}
        </span>
      ))}
      {value.length < 6 && (
        <button type="button" className="secondary small" onClick={() => onChange([...value, ''])}>+ Add time</button>
      )}
    </div>
  );
}
