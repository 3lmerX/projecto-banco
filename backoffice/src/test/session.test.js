import test from 'node:test';
import assert from 'node:assert/strict';
import {
  clearSession,
  decodeJwtPayload,
  getSession,
  isTokenValid,
  setSession,
  tokenExpirationMs,
} from '../auth/session.js';

function jwt(payload) {
  const encode = (value) => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'none' })}.${encode(payload)}.signature`;
}

test('reads an expiration from the JWT payload', () => {
  const token = jwt({ exp: 2_000_000_000 });
  assert.equal(tokenExpirationMs(token), 2_000_000_000_000);
  assert.equal(isTokenValid(token, 1_999_999_999_000), true);
});

test('rejects expired and malformed JWTs', () => {
  assert.equal(isTokenValid(jwt({ exp: 100 }), 100_000), false);
  assert.equal(isTokenValid('not-a-jwt', 0), false);
  assert.equal(decodeJwtPayload('broken'), null);
});

test('persists staff session details and clears them on logout', () => {
  const values = new Map();
  globalThis.localStorage = {
    getItem: (key) => values.get(key) || null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  };
  try {
    const response = {
      token: jwt({ exp: Math.floor(Date.now() / 1000) + 3600 }),
      nome: 'Funcionário',
      role: 'ROLE_FUNCIONARIO',
      banco: 'Banco ISCIM',
    };
    const session = setSession(response);
    assert.equal(JSON.parse(values.get('banco-backoffice-session')).user.nome, 'Funcionário');
    assert.equal(getSession().token, session.token);
    clearSession();
    assert.equal(values.has('banco-backoffice-session'), false);
    assert.equal(getSession(), null);
  } finally {
    delete globalThis.localStorage;
  }
});
