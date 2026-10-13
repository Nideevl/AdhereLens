const ZONE = 'Asia/Kolkata'; // the hospital time zone the backend uses for dose times

export function fmtDateTime(iso: string | null): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleString('en-IN', {
    timeZone: ZONE, day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true,
  });
}

export function fmtTime(hhmmss: string): string {
  return hhmmss.slice(0, 5);
}

export function fmtDate(iso: string | null): string {
  if (!iso) return '-';
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function ageYears(dob: string): number {
  const [y, m, d] = dob.split('-').map(Number);
  const now = new Date();
  let age = now.getFullYear() - y;
  if (now.getMonth() + 1 < m || (now.getMonth() + 1 === m && now.getDate() < d)) age--;
  return age;
}

export function label(value: string): string {
  const text = value.replace(/_/g, ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function todayIso(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: ZONE });
}

/** Coded values are upper-case letters, digits and underscore only. */
export function toCode(value: string): string {
  return value.toUpperCase().replace(/[^A-Z0-9_]+/g, '_');
}

export function numOrNull(value: string): number | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : Number(trimmed);
}

export function textOrNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}
