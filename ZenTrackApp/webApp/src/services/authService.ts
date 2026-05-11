import type { components } from '../types/api';

type LoginRequest = components['schemas']['me.dcueto.zentrackapp.dto.LoginRequest'];
type RegisterRequest = components['schemas']['me.dcueto.zentrackapp.dto.RegisterRequest'];
type AuthResponse = components['schemas']['me.dcueto.zentrackapp.dto.AuthResponse'];
type ErrorResponse = components['schemas']['me.dcueto.zentrackapp.api.ErrorResponse'];

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export type AuthResult =
  | { success: true; data: AuthResponse }
  | { success: false; error: string };

async function parseAuthResponse(res: Response): Promise<AuthResult> {
  const json = await res.json();
  if (res.ok) return { success: true, data: json as AuthResponse };
  return { success: false, error: (json as ErrorResponse).error ?? 'Error desconocido' };
}

export async function login(body: LoginRequest): Promise<AuthResult> {
  const res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseAuthResponse(res);
}

export async function register(body: RegisterRequest): Promise<AuthResult> {
  const res = await fetch(`${BASE_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseAuthResponse(res);
}

export async function exchangeGoogleCode(body: { code: string; state: string }): Promise<AuthResult> {
  const res = await fetch(`${BASE_URL}/api/auth/google/exchange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseAuthResponse(res);
}
