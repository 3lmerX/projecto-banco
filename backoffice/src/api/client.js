import { clearSession, getSession } from '../auth/session.js';

const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(status, message, payload = null) {
    super(message || `O pedido falhou (HTTP ${status}).`);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

async function request(path, { method = 'GET', body, query, unauthenticated = false } = {}) {
  const url = new URL(`${API_URL}${path}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== '' && value !== null && value !== undefined) url.searchParams.set(key, String(value));
    }
  }

  const headers = { Accept: 'application/json' };
  const session = getSession();
  if (!unauthenticated && session?.token) headers.Authorization = `Bearer ${session.token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
  } catch {
    throw new ApiError(0, 'Não foi possível contactar o serviço. Verifique a ligação e tente novamente.');
  }

  if (response.status === 401 && !unauthenticated) {
    clearSession();
    window.dispatchEvent(new Event('session-expired'));
  }
  if (response.status === 204) return null;

  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = null;
    }
  }
  if (!response.ok) {
    throw new ApiError(response.status, payload?.message || payload?.error?.message || `O pedido falhou (HTTP ${response.status}).`, payload);
  }
  return payload;
}

const segment = (value) => encodeURIComponent(String(value));

export const api = {
  login: (credentials) => request('/auth/login/funcionario', { method: 'POST', body: credentials, unauthenticated: true }),
  createFuncionario: (input) => request('/auth/funcionarios', { method: 'POST', body: input }),
  listFuncionarios: () => request('/auth/funcionarios'),
  createCliente: (input) => request('/clientes', { method: 'POST', body: input }),
  listClientes: (q = '') => request('/clientes', { query: { q } }),
  getCliente: (id) => request(`/clientes/${segment(id)}`),
  updateCliente: (id, input) => request(`/clientes/${segment(id)}`, { method: 'PUT', body: input }),
  listContasCliente: (id) => request(`/clientes/${segment(id)}/contas`),
  openConta: (id, input) => request(`/clientes/${segment(id)}/contas`, { method: 'POST', body: input }),
  listContas: (q = '') => request('/contas', { query: { q } }),
  getConta: (numero) => request(`/contas/${segment(numero)}`),
  setContaBlocked: (numero, blocked) => request(`/contas/${segment(numero)}/${blocked ? 'bloquear' : 'desbloquear'}`, { method: 'PATCH' }),
  depositar: (numero, valor) => request(`/contas/${segment(numero)}/depositos`, { method: 'POST', body: { valor } }),
  listMovimentos: (numero, limite = 20) => request(`/contas/${segment(numero)}/movimentos`, { query: { limite } }),
  listAuditoria: (filters) => request('/auditoria', { query: filters }),
};

export function getApiBaseUrl() {
  return API_URL;
}
