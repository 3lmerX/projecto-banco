const SESSION_STORAGE_KEY = 'banco-backoffice-session';

function readStoredSession() {
  try {
    const stored = JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY));
    if (stored?.token && stored?.expiresAt > Date.now()) return stored;
  } catch {
    return null;
  }
  localStorage.removeItem(SESSION_STORAGE_KEY);
  return null;
}

let currentSession = readStoredSession();

export function decodeJwtPayload(token) {
  try {
    const encoded = token.split('.')[1];
    if (!encoded) return null;
    const base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function tokenExpirationMs(token) {
  const exp = decodeJwtPayload(token)?.exp;
  return Number.isFinite(exp) ? exp * 1000 : null;
}

export function isTokenValid(token, now = Date.now()) {
  const expiresAt = tokenExpirationMs(token);
  return Boolean(token && expiresAt && expiresAt > now);
}

export function setSession(loginResponse) {
  const token = loginResponse?.token;
  const expiresAt = tokenExpirationMs(token);
  if (!token || !expiresAt || expiresAt <= Date.now()) {
    throw new Error('A sessão recebida é inválida ou já expirou. Inicie sessão novamente.');
  }
  currentSession = {
    token,
    expiresAt,
    user: {
      nome: loginResponse.nome,
      role: loginResponse.role,
      banco: loginResponse.banco,
      primeiroAcesso: loginResponse.primeiroAcesso,
    },
  };
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(currentSession));
  return currentSession;
}

export function getSession() {
  if (currentSession && currentSession.expiresAt <= Date.now()) clearSession();
  return currentSession;
}

export function clearSession() {
  currentSession = null;
  localStorage.removeItem(SESSION_STORAGE_KEY);
}

export function millisecondsUntilExpiry() {
  const session = getSession();
  return session ? Math.max(0, session.expiresAt - Date.now()) : 0;
}
