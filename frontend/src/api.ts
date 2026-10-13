import type { AuthState } from './types';

const KEY = 'adherelens.auth';

export function loadAuth(): AuthState | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as AuthState) : null;
  } catch {
    return null;
  }
}
export function saveAuth(auth: AuthState) {
  localStorage.setItem(KEY, JSON.stringify(auth));
}
export function clearAuth() {
  localStorage.removeItem(KEY);
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn;
}

export async function api<T>(method: string, path: string, body?: unknown): Promise<T> {
  const auth = loadAuth();
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth) headers['Authorization'] = `Bearer ${auth.token}`;

  const res = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && path !== '/api/auth/login') {
    clearAuth();
    onUnauthorized?.();
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const data = await res.json();
      if (data && typeof data.error === 'string' && res.status !== 403) message = data.error;
    } catch {
      /* body was empty or not JSON */
    }
    if (res.status === 401) message = path === '/api/auth/login' ? 'Wrong email or password' : 'Session expired. Please log in again';
    else if (res.status === 403) message = 'You do not have permission for this action';
    else if (res.status === 400 && message.startsWith('Request failed')) message = 'Invalid input. Check the fields and try again';
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const get = <T,>(path: string) => api<T>('GET', path);
export const post = <T,>(path: string, body?: unknown) => api<T>('POST', path, body);
export const put = <T,>(path: string, body?: unknown) => api<T>('PUT', path, body);
export const patch = <T,>(path: string, body?: unknown) => api<T>('PATCH', path, body);
