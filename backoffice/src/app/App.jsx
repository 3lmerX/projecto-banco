import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import {
  Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams,
  useSearchParams,
} from 'react-router-dom';
import { api } from '../api/client.js';
import '../components/ClienteFormModal.css';
import {
  clearSession, getSession, millisecondsUntilExpiry, setSession,
} from '../auth/session.js';
import { formatDate, formatDateOnly, formatMoney } from '../utils/format.js';

const AuthContext = createContext(null);
const ToastContext = createContext(() => {});
const BANK_NAME = import.meta.env.VITE_BANCO_NOME || 'Banco ISCIM';
const displayBankName = (name) => name && name !== '[NOME_DO_BANCO]' ? name : BANK_NAME;

function AuthProvider({ children }) {
  const [session, setSessionState] = useState(() => getSession());
  const navigate = useNavigate();

  useEffect(() => {
    if (!session) return undefined;
    const timer = window.setTimeout(() => {
      if (millisecondsUntilExpiry() > 0) {
        setSessionState({ ...session });
        return;
      }
      clearSession();
      setSessionState(null);
      navigate('/login', { replace: true, state: { message: 'A sua sessão expirou. Inicie sessão novamente.' } });
    }, Math.min(millisecondsUntilExpiry(), 2_147_000_000));
    return () => window.clearTimeout(timer);
  }, [session, navigate]);

  useEffect(() => {
    const expire = () => {
      clearSession();
      setSessionState(null);
      navigate('/login', { replace: true, state: { message: 'A sessão expirou. Inicie sessão novamente.' } });
    };
    window.addEventListener('session-expired', expire);
    return () => window.removeEventListener('session-expired', expire);
  }, [navigate]);

  const value = useMemo(() => ({
    session,
    login: (response) => {
      if (!['ROLE_FUNCIONARIO', 'ROLE_ADMIN'].includes(response?.role)) {
        clearSession();
        throw new Error('Acesso reservado a funcionários');
      }
      const current = setSession(response);
      setSessionState(current);
    },
    logout: () => {
      clearSession();
      setSessionState(null);
      navigate('/login', { replace: true });
    },
  }), [session, navigate]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function useAuth() {
  return useContext(AuthContext);
}

function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(null), 4200);
    return () => window.clearTimeout(timer);
  }, [toast]);
  return (
    <ToastContext.Provider value={setToast}>
      {children}
      {toast && (
        <div className={`toast toast--${toast.kind || 'success'}`} role="status" aria-live="polite">
          <span>{toast.message}</span>
          <button className="toast__close" aria-label="Fechar notificação" onClick={() => setToast(null)}>×</button>
        </div>
      )}
    </ToastContext.Provider>
  );
}

function useToast() {
  return useContext(ToastContext);
}

function RequireAuth({ children }) {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) return <Navigate to="/login" replace state={{ from: location }} />;
  return children;
}

function RequireAdmin({ children }) {
  const { session } = useAuth();
  if (session?.user?.role !== 'ROLE_ADMIN') return <Navigate to="/" replace />;
  return children;
}

function PageTitle({ eyebrow, title, description, actions }) {
  return (
    <div className="page-title">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      {actions && <div className="page-title__actions">{actions}</div>}
    </div>
  );
}

function Notice({ kind = 'error', title, children, action }) {
  return (
    <section className={`notice notice--${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      <div>
        {title && <strong>{title}</strong>}
        {children && <p>{children}</p>}
      </div>
      {action}
    </section>
  );
}

function Loading({ label = 'A carregar dados…' }) {
  return (
    <div className="loading-block" role="status" aria-live="polite" aria-busy="true">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

function EmptyState({ title = 'Sem resultados', children }) {
  return <div className="empty-state"><span className="empty-icon" aria-hidden="true">⌕</span><strong>{title}</strong><p>{children}</p></div>;
}

function Field({ label, hint, ...props }) {
  return (
    <label className="field">
      <span className="field__label">{label}</span>
      <input {...props} />
      {hint && <span className="field__hint">{hint}</span>}
    </label>
  );
}

function ClienteFormModal({ initialData, onClose, onSave }) {
  const [values, setValues] = useState({
    nome: initialData?.nome || '',
    numeroBi: initialData?.numeroBi || '',
    telefone: initialData?.telefone || '',
    email: initialData?.email || '',
    estado: initialData?.estado || 'ACTIVO',
  });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  function update(field, value) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError('');
    if (!values.nome.trim() || !values.numeroBi.trim() || !values.telefone.trim()) {
      setError('Nome, número do BI e telefone são obrigatórios.');
      return;
    }
    if (values.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      setError('Indique um endereço de e-mail válido.');
      return;
    }
    const payload = {
      nome: values.nome.trim(),
      numeroBi: values.numeroBi.trim(),
      telefone: values.telefone.trim(),
      email: values.email.trim() || null,
      ...(initialData ? { estado: values.estado } : {}),
    };
    setSaving(true);
    try {
      await onSave(payload);
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal panel" role="dialog" aria-modal="true" aria-labelledby="cliente-modal-title">
        <div className="section-heading">
          <div><p className="eyebrow">GESTÃO DE CLIENTES</p><h2 id="cliente-modal-title">{initialData ? 'Editar cliente' : 'Novo cliente'}</h2></div>
          <button type="button" className="button button--quiet button--small" aria-label="Fechar formulário" onClick={onClose}>×</button>
        </div>
        {error && <Notice>{error}</Notice>}
        <form className="form-grid" onSubmit={submit}>
          <Field label="Nome completo" autoFocus required maxLength="160" value={values.nome} onChange={(event) => update('nome', event.target.value)} />
          <Field label="Número do BI" required maxLength="80" value={values.numeroBi} onChange={(event) => update('numeroBi', event.target.value)} />
          <Field label="Telefone" type="tel" required maxLength="40" value={values.telefone} onChange={(event) => update('telefone', event.target.value)} />
          <Field label="E-mail" type="email" maxLength="160" value={values.email} onChange={(event) => update('email', event.target.value)} />
          {initialData && <label className="field"><span className="field__label">Estado</span><select value={values.estado} onChange={(event) => update('estado', event.target.value)}><option value="ACTIVO">Activo</option><option value="INACTIVO">Inactivo</option></select></label>}
          <div className="form-actions"><button type="button" className="button button--outline" onClick={onClose} disabled={saving}>Cancelar</button><button className="button button--primary" type="submit" disabled={saving}>{saving ? 'A guardar…' : 'Guardar cliente'}</button></div>
        </form>
      </section>
    </div>
  );
}

function StatusBadge({ value }) {
  const normalized = String(value || '—').toUpperCase();
  const kind = /ACTIV|CONCLU|SUCESS|CREDITO/.test(normalized) ? 'success'
    : /BLOQUE|INACT|REJEIT|FALH/.test(normalized) ? 'danger' : 'neutral';
  return <span className={`badge badge--${kind}`}>{value || '—'}</span>;
}

function DataTable({ headers, children, label }) {
  return (
    <div className="table-scroll">
      <table>
        <caption className="sr-only">{label}</caption>
        <thead><tr>{headers.map((header) => <th scope="col" key={header}>{header}</th>)}</tr></thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}

function AppShell() {
  const { session, logout } = useAuth();
  const location = useLocation();
  const active = location.pathname === '/' ? '/' : `/${location.pathname.split('/')[1]}`;
  const navigation = [
    { to: '/', label: 'Visão geral', icon: '⌂', end: true },
    { to: '/clientes', label: 'Clientes', icon: '♙' },
    { to: '/contas', label: 'Contas', icon: '▤' },
    { to: '/depositos', label: 'Depósitos', icon: '＋' },
    { to: '/extracto', label: 'Extracto', icon: '≡' },
    { to: '/auditoria', label: 'Auditoria', icon: '◷' },
    ...(session.user.role === 'ROLE_ADMIN' ? [{ to: '/funcionarios', label: 'Funcionários', icon: '♧' }] : []),
  ];
  return (
    <div className="app-shell">
      <a className="skip-link" href="#conteudo">Saltar para o conteúdo</a>
      <header className="topbar">
        <Link className="brand" to="/" aria-label={`${BANK_NAME} — página inicial`}>
          <span className="brand__mark" aria-hidden="true">B</span>
          <span><strong>{BANK_NAME}</strong><small>Operações bancárias</small></span>
        </Link>
        <div className="topbar__right">
          <div className="staff-chip"><span className="staff-chip__avatar" aria-hidden="true">{session.user.nome?.slice(0, 1) || 'F'}</span><span>{session.user.nome || 'Funcionário'}</span></div>
          <button className="button button--quiet button--small" onClick={logout}>Sair</button>
        </div>
      </header>
      <div className="workspace">
        <aside className="sidebar" aria-label="Navegação principal">
          <p className="sidebar__label">MENU PRINCIPAL</p>
          <nav>
            {navigation.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`} aria-current={active === item.to ? 'page' : undefined}>
                <span className="nav-link__icon" aria-hidden="true">{item.icon}</span><span>{item.label}</span>
              </NavLink>
            ))}
          </nav>
          <div className="sidebar__footer"><span className="online-dot" /> Ligação à API configurada</div>
        </aside>
        <main id="conteudo" className="main-content" tabIndex="-1">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/clientes" element={<Clientes />} />
            <Route path="/clientes/:id" element={<ClienteDetalhe />} />
            <Route path="/contas" element={<Contas />} />
            <Route path="/depositos" element={<Depositos />} />
            <Route path="/extracto" element={<Extracto />} />
            <Route path="/auditoria" element={<Auditoria />} />
            <Route path="/funcionarios" element={<RequireAdmin><Funcionarios /></RequireAdmin>} />
            <Route path="*" element={<NotFound />} />
          </Routes>
          <footer className="app-footer"><span>{BANK_NAME} · Backoffice</span><span>Dados apresentados conforme respostas dos serviços.</span></footer>
        </main>
      </div>
    </div>
  );
}

function Dashboard() {
  const [state, setState] = useState({ loading: true, error: '', clientes: [], contas: [] });
  const load = () => {
    setState((current) => ({ ...current, loading: true, error: '' }));
    Promise.all([api.listClientes(), api.listContas()])
      .then(([clientes, contas]) => setState({ loading: false, error: '', clientes, contas }))
      .catch((error) => setState({ loading: false, error: error.message, clientes: [], contas: [] }));
  };
  useEffect(load, []);
  const saldoTotal = state.contas.reduce((sum, conta) => sum + Number(conta.saldo || 0), 0);
  return (
    <>
      <PageTitle eyebrow="CENTRO DE OPERAÇÕES" title="Visão geral" description="Resumo actualizado a partir dos serviços do banco." />
      {state.error && <Notice title="Não foi possível carregar o resumo" action={<button className="button button--outline button--small" onClick={load}>Tentar novamente</button>}>{state.error}</Notice>}
      <section className="welcome-panel">
        <div><span className="welcome-panel__eyebrow">Bem-vindo ao backoffice</span><h2>Operações claras, com dados em tempo real.</h2><p>Consulte clientes e contas ou registe uma operação bancária autorizada.</p></div>
        <span className="welcome-panel__graphic" aria-hidden="true">◈</span>
      </section>
      {state.loading ? <Loading label="A consultar clientes e contas…" /> : !state.error && (
        <section className="metric-grid" aria-label="Indicadores do banco">
          <article className="metric-card"><span className="metric-card__icon metric-card__icon--blue" aria-hidden="true">♙</span><span className="metric-card__label">Clientes listados</span><strong>{state.clientes.length.toLocaleString('pt-MZ')}</strong><span className="metric-card__note">Registos devolvidos pela API</span></article>
          <article className="metric-card"><span className="metric-card__icon metric-card__icon--orange" aria-hidden="true">▤</span><span className="metric-card__label">Contas listadas</span><strong>{state.contas.length.toLocaleString('pt-MZ')}</strong><span className="metric-card__note">Registos devolvidos pela API</span></article>
          <article className="metric-card"><span className="metric-card__icon metric-card__icon--gold" aria-hidden="true">MZN</span><span className="metric-card__label">Saldo total listado</span><strong>{formatMoney(saldoTotal)}</strong><span className="metric-card__note">Soma dos saldos recebidos</span></article>
        </section>
      )}
      <section className="panel quick-panel">
        <div className="section-heading"><div><h2>Acesso rápido</h2><p>Atalhos para as tarefas mais frequentes.</p></div></div>
        <div className="quick-links">
          <Link to="/clientes" className="quick-link"><span className="quick-link__icon">♙</span><span><strong>Pesquisar clientes</strong><small>Consultar dados e contas</small></span><span className="quick-link__arrow" aria-hidden="true">→</span></Link>
          <Link to="/depositos" className="quick-link"><span className="quick-link__icon quick-link__icon--orange">＋</span><span><strong>Registar depósito</strong><small>Depositar numa conta</small></span><span className="quick-link__arrow" aria-hidden="true">→</span></Link>
          <Link to="/auditoria" className="quick-link"><span className="quick-link__icon quick-link__icon--gold">◷</span><span><strong>Ver auditoria</strong><small>Consultar histórico de acções</small></span><span className="quick-link__arrow" aria-hidden="true">→</span></Link>
        </div>
      </section>
    </>
  );
}

function Clientes() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [state, setState] = useState({ loading: true, error: '', data: [] });
  const [createOpen, setCreateOpen] = useState(false);
  const toast = useToast();
  const load = (q = query.trim()) => {
    setState((current) => ({ ...current, loading: true, error: '' }));
    api.listClientes(q).then((data) => setState({ loading: false, error: '', data }))
      .catch((error) => setState((current) => ({ ...current, loading: false, error: error.message })));
  };
  useEffect(() => {
    const timer = window.setTimeout(() => load(query.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [query, refresh]);
  async function createCliente(values) {
    await api.createCliente(values);
    setCreateOpen(false);
    setRefresh((value) => value + 1);
    toast({ message: 'Cliente criado com sucesso.' });
  }
  return (
    <>
      <PageTitle eyebrow="RELACIONAMENTO" title="Clientes" description="Pesquise e consulte os dados de clientes registados." actions={<button className="button button--primary" onClick={() => setCreateOpen(true)}>Novo cliente</button>} />
      <section className="panel">
        <div className="search-form" role="search">
          <Field label="Pesquisar clientes" placeholder="Nome, BI, telefone ou e-mail" value={query} onChange={(event) => setQuery(event.target.value)} />
        </div>
      </section>
      {state.loading ? <Loading /> : state.error ? <Notice title="Erro ao carregar clientes" action={<button className="button button--outline button--small" onClick={() => load(query.trim())}>Tentar novamente</button>}>{state.error}</Notice>
        : state.data.length === 0 ? <EmptyState title="Nenhum cliente encontrado">Não existem registos para apresentar com esta pesquisa.</EmptyState>
          : <section className="panel table-panel"><div className="section-heading"><div><h2>Resultados</h2><p>{state.data.length} registo(s) devolvido(s)</p></div></div>
            <DataTable label="Lista de clientes" headers={['Cliente', 'Número de BI', 'Telefone', 'E-mail', 'Estado', '']}>
              {state.data.map((cliente) => {
                const detailPath = `/clientes/${encodeURIComponent(cliente.id)}`;
                return <tr key={cliente.id} tabIndex={0} aria-label={`Abrir detalhe de ${cliente.nome}`} onClick={() => navigate(detailPath)} onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    navigate(detailPath);
                  }
                }}><td><strong>{cliente.nome}</strong></td><td>{cliente.numeroBi || '—'}</td><td>{cliente.telefone || '—'}</td><td>{cliente.email || '—'}</td><td><StatusBadge value={cliente.estado} /></td><td><Link className="table-link" to={detailPath} onClick={(event) => event.stopPropagation()}>Ver detalhe <span aria-hidden="true">→</span></Link></td></tr>;
              })}
            </DataTable>
          </section>}
      {createOpen && <ClienteFormModal onClose={() => setCreateOpen(false)} onSave={createCliente} />}
    </>
  );
}

function Funcionarios() {
  const toast = useToast();
  const [values, setValues] = useState({ nome: '', email: '', palavraPasse: '', estado: 'ACTIVO' });
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [listState, setListState] = useState({ loading: true, error: '', data: [] });

  async function loadFuncionarios() {
    setListState((current) => ({ ...current, loading: true, error: '' }));
    try {
      const data = await api.listFuncionarios();
      setListState({ loading: false, error: '', data });
    } catch (loadError) {
      setListState((current) => ({ ...current, loading: false, error: loadError.message }));
    }
  }

  useEffect(() => {
    loadFuncionarios();
  }, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    if (values.palavraPasse !== confirmation) {
      setError('A confirmação da palavra-passe não corresponde.');
      return;
    }
    setSaving(true);
    try {
      const funcionario = await api.createFuncionario({
        ...values,
        nome: values.nome.trim(),
        email: values.email.trim(),
      });
      setValues({ nome: '', email: '', palavraPasse: '', estado: 'ACTIVO' });
      setConfirmation('');
      toast({ message: `Funcionário ${funcionario.nome} registado com sucesso.` });
      await loadFuncionarios();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageTitle eyebrow="ADMINISTRAÇÃO" title="Funcionários" description="Registe e consulte as contas de acesso dos funcionários do banco." />
      <section className="panel">
        <div className="section-heading"><div><h2>Registar funcionário</h2><p>Crie uma nova conta de acesso.</p></div></div>
        {error && <Notice title="Não foi possível registar o funcionário">{error}</Notice>}
        <form className="form-grid" onSubmit={submit}>
          <Field label="Nome completo" autoComplete="name" required maxLength="160" value={values.nome} onChange={(event) => setValues((current) => ({ ...current, nome: event.target.value }))} />
          <Field label="E-mail de acesso" type="email" autoComplete="email" required maxLength="160" value={values.email} onChange={(event) => setValues((current) => ({ ...current, email: event.target.value }))} />
          <Field label="Palavra-passe inicial" type="password" autoComplete="new-password" required minLength="8" maxLength="72" value={values.palavraPasse} onChange={(event) => setValues((current) => ({ ...current, palavraPasse: event.target.value }))} />
          <Field label="Confirmar palavra-passe" type="password" autoComplete="new-password" required minLength="8" maxLength="72" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />
          <label className="field">
            <span className="field__label">Estado</span>
            <select value={values.estado} onChange={(event) => setValues((current) => ({ ...current, estado: event.target.value }))}>
              <option value="ACTIVO">Activo</option>
              <option value="BLOQUEADO">Bloqueado</option>
            </select>
          </label>
          <div className="form-actions">
            <button className="button button--primary" type="submit" disabled={saving}>{saving ? 'A registar…' : 'Registar funcionário'}</button>
          </div>
        </form>
      </section>
      {listState.loading ? <Loading label="A carregar funcionários…" />
        : listState.error ? <Notice title="Erro ao carregar funcionários" action={<button className="button button--outline button--small" onClick={loadFuncionarios}>Tentar novamente</button>}>{listState.error}</Notice>
          : listState.data.length === 0 ? <EmptyState title="Nenhum funcionário encontrado">Ainda não existem funcionários registados.</EmptyState>
            : <section className="panel table-panel"><div className="section-heading"><div><h2>Funcionários registados</h2><p>{listState.data.length} funcionário(s)</p></div><button className="button button--outline button--small" onClick={loadFuncionarios}>Actualizar lista</button></div>
              <DataTable label="Funcionários registados" headers={['Nome', 'E-mail', 'Estado']}>
                {listState.data.map((funcionario) => (
                  <tr key={funcionario.id}>
                    <td><strong>{funcionario.nome}</strong></td>
                    <td>{funcionario.email}</td>
                    <td><StatusBadge value={funcionario.estado} /></td>
                  </tr>
                ))}
              </DataTable>
            </section>}
    </>
  );
}

function ClienteDetalhe() {
  const { id } = useParams();
  const toast = useToast();
  const [refresh, setRefresh] = useState(0);
  const [clientState, setClientState] = useState({ loading: true, error: '', data: null });
  const [accountsState, setAccountsState] = useState({ loading: true, error: '', data: [] });
  const [pin, setPin] = useState('');
  const [pinConfirmation, setPinConfirmation] = useState('');
  const [openingBalance, setOpeningBalance] = useState('');
  const [opening, setOpening] = useState(false);
  const [formError, setFormError] = useState('');
  const [card, setCard] = useState(null);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    let active = true;
    setClientState({ loading: true, error: '', data: null });
    setAccountsState({ loading: true, error: '', data: [] });
    Promise.all([api.getCliente(id), api.listContasCliente(id)])
      .then(([data, accounts]) => {
        if (!active) return;
        setClientState({ loading: false, error: '', data });
        setAccountsState({ loading: false, error: '', data: accounts });
      })
      .catch((error) => {
        if (!active) return;
        setClientState({ loading: false, error: error.message, data: null });
        setAccountsState({ loading: false, error: error.message, data: [] });
      });
    return () => { active = false; };
  }, [id, refresh]);

  async function submitOpenAccount(event) {
    event.preventDefault();
    setFormError('');
    const balance = Number(openingBalance.replace(',', '.'));
    if (!/^\d{4}$/.test(pin)) {
      setFormError('O PIN inicial deve conter exactamente 4 dígitos.');
      return;
    }
    if (pin !== pinConfirmation) {
      setFormError('A confirmação do PIN não corresponde.');
      return;
    }
    if (!Number.isFinite(balance) || balance < 0) {
      setFormError('Indique um saldo inicial válido.');
      return;
    }
    if (!window.confirm(`Confirmar a abertura de uma conta para ${clientState.data.nome}?`)) return;
    setOpening(true);
    try {
      const account = await api.openConta(id, { pin, saldoInicial: balance });
      setCard({ account, pin });
      setPin('');
      setPinConfirmation('');
      setOpeningBalance('');
      setRefresh((value) => value + 1);
      toast({ message: 'Conta aberta com sucesso. Consulte o cartão temporário.' });
    } catch (error) {
      setFormError(error.message);
    } finally {
      setOpening(false);
    }
  }

  async function updateCliente(values) {
    const updated = await api.updateCliente(id, values);
    setClientState({ loading: false, error: '', data: updated });
    setEditing(false);
    toast({ message: 'Dados do cliente actualizados com sucesso.' });
  }

  if (clientState.loading) return <><PageTitle title="Detalhe do cliente" /><Loading /></>;
  if (clientState.error) return <><PageTitle title="Detalhe do cliente" /><Notice title="Não foi possível carregar o cliente" action={<button className="button button--outline button--small" onClick={() => setRefresh((v) => v + 1)}>Tentar novamente</button>}>{clientState.error}</Notice><Link to="/clientes" className="text-link">← Voltar aos clientes</Link></>;
  const cliente = clientState.data;
  return (
    <>
      <PageTitle eyebrow="CLIENTES" title={cliente.nome} description={`Perfil do cliente · Registo #${cliente.id}`} actions={<><button className="button button--primary" onClick={() => setEditing(true)}>Editar cliente</button><Link className="button button--outline" to="/clientes">← Voltar</Link></>} />
      <section className="profile-grid">
        <article className="panel profile-card"><div className="profile-avatar" aria-hidden="true">{cliente.nome?.slice(0, 1) || 'C'}</div><div><h2>{cliente.nome}</h2><StatusBadge value={cliente.estado} /></div><dl className="details-list"><div><dt>Número de BI</dt><dd>{cliente.numeroBi || '—'}</dd></div><div><dt>Telefone</dt><dd>{cliente.telefone || '—'}</dd></div><div><dt>E-mail</dt><dd>{cliente.email || '—'}</dd></div><div><dt>Identificador</dt><dd>{cliente.id}</dd></div></dl></article>
        <section className="panel open-account-panel"><div className="section-heading"><div><h2>Abrir conta</h2><p>Os dados do PIN não ficam guardados após fechar o cartão.</p></div><span className="section-icon" aria-hidden="true">＋</span></div>
          {formError && <Notice>{formError}</Notice>}
          <form onSubmit={submitOpenAccount} className="form-grid">
            <Field label="PIN inicial" type="password" autoComplete="new-password" required minLength="4" maxLength="4" pattern="[0-9]{4}" inputMode="numeric" value={pin} onChange={(event) => setPin(event.target.value.replace(/\D/g, '').slice(0, 4))} hint="Introduza exactamente 4 dígitos. Só será mostrado no cartão temporário." />
            <Field label="Confirmar PIN inicial" type="password" autoComplete="new-password" required minLength="4" maxLength="4" pattern="[0-9]{4}" inputMode="numeric" value={pinConfirmation} onChange={(event) => setPinConfirmation(event.target.value.replace(/\D/g, '').slice(0, 4))} />
            <Field label="Saldo inicial (MZN)" type="number" min="0" step="0.01" required value={openingBalance} onChange={(event) => setOpeningBalance(event.target.value)} placeholder="0,00" />
            <div className="form-actions"><button className="button button--primary" type="submit" disabled={opening}>{opening ? 'A abrir conta…' : 'Abrir conta'}</button></div>
          </form>
        </section>
      </section>
      {editing && <ClienteFormModal initialData={cliente} onClose={() => setEditing(false)} onSave={updateCliente} />}
      {card && <section className="panel open-card-result" aria-live="polite">
        <div className="section-heading"><div><p className="eyebrow">CONTA ABERTA</p><h2>Cartão de abertura</h2><p>O PIN é temporário e só permanece nesta vista enquanto o cartão estiver aberto.</p></div><button className="button button--quiet button--small no-print" onClick={() => setCard(null)}>Fechar cartão</button></div>
        <article className="account-card print-card">
          <div className="account-card__header"><span className="brand__mark">B</span><strong>{displayBankName(card.account.banco)}</strong><span className="account-card__tag">CONTA</span></div>
          <div className="account-card__number-label">Número da conta</div><div className="account-card__number">{card.account.numero}</div>
          <div className="account-card__bottom"><div><small>TITULAR</small><strong>{card.account.clienteNome || cliente.nome}</strong></div><div><small>PIN INICIAL</small><strong className="account-card__pin">{card.pin}</strong></div></div>
          <p className="account-card__warning">O cliente deve alterar o PIN no primeiro acesso.</p>
        </article>
        <div className="form-actions no-print"><button className="button button--primary" onClick={() => window.print()}>Imprimir cartão</button></div>
      </section>}
      <section className="panel table-panel"><div className="section-heading"><div><h2>Contas do cliente</h2><p>Contas associadas a este perfil.</p></div><button className="button button--outline button--small" onClick={() => setRefresh((v) => v + 1)}>Actualizar</button></div>
        {accountsState.loading ? <Loading label="A consultar contas…" /> : accountsState.error ? <Notice title="Não foi possível carregar as contas">{accountsState.error}</Notice>
          : accountsState.data.length === 0 ? <EmptyState title="Sem contas associadas">Este cliente não tem contas devolvidas pelo serviço.</EmptyState>
            : <DataTable label="Contas do cliente" headers={['Número', 'Saldo', 'Estado', 'Data de abertura', '']}>{accountsState.data.map((account) => <tr key={account.id || account.numero}><td><strong className="mono">{account.numero}</strong></td><td>{formatMoney(account.saldo)}</td><td><StatusBadge value={account.estado} /></td><td>{formatDate(account.dataAbertura)}</td><td><Link className="table-link" to={`/extracto?numero=${encodeURIComponent(account.numero)}`}>Ver extracto →</Link></td></tr>)}</DataTable>}
      </section>
    </>
  );
}

function Contas() {
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [state, setState] = useState({ loading: true, error: '', data: [] });
  const [busyAccount, setBusyAccount] = useState('');
  const toast = useToast();
  const load = (q = submitted) => {
    setState((current) => ({ ...current, loading: true, error: '' }));
    api.listContas(q).then((data) => setState({ loading: false, error: '', data }))
      .catch((error) => setState((current) => ({ ...current, loading: false, error: error.message })));
  };
  useEffect(() => { load(''); }, []);
  function submit(event) {
    event.preventDefault();
    setSubmitted(query.trim());
    load(query.trim());
  }
  async function toggleBlocked(account) {
    const isBlocked = String(account.estado).toUpperCase().includes('BLOQUE');
    const action = isBlocked ? 'desbloquear' : 'bloquear';
    if (!window.confirm(`Tem a certeza de que pretende ${action} a conta ${account.numero}?`)) return;
    setBusyAccount(account.numero);
    try {
      await api.setContaBlocked(account.numero, !isBlocked);
      toast({ message: `Conta ${account.numero} ${isBlocked ? 'desbloqueada' : 'bloqueada'} com sucesso.` });
      load();
    } catch (error) {
      toast({ kind: 'error', message: error.message });
    } finally {
      setBusyAccount('');
    }
  }
  return (
    <>
      <PageTitle eyebrow="GESTÃO DE CONTAS" title="Contas" description="Pesquise pelo número da conta ou nome do titular." />
      <section className="panel"><form className="search-form" onSubmit={submit} role="search"><Field label="Pesquisar contas" placeholder="Número da conta ou titular" value={query} onChange={(event) => setQuery(event.target.value)} /><button className="button button--primary" type="submit">Pesquisar</button></form></section>
      {state.loading ? <Loading /> : state.error ? <Notice title="Erro ao carregar contas" action={<button className="button button--outline button--small" onClick={() => load()}>Tentar novamente</button>}>{state.error}</Notice>
        : state.data.length === 0 ? <EmptyState title="Nenhuma conta encontrada">Não existem contas para apresentar com esta pesquisa.</EmptyState>
          : <section className="panel table-panel"><div className="section-heading"><div><h2>Contas encontradas</h2><p>{state.data.length} registo(s) devolvido(s)</p></div></div>
            <DataTable label="Lista de contas" headers={['Número da conta', 'Titular', 'Saldo', 'Estado', 'Abertura', 'Acções']}>{state.data.map((account) => {
              const isBlocked = String(account.estado).toUpperCase().includes('BLOQUE');
              return <tr key={account.id || account.numero}><td><strong className="mono">{account.numero}</strong></td><td>{account.clienteNome || '—'}</td><td>{formatMoney(account.saldo)}</td><td><StatusBadge value={account.estado} /></td><td>{formatDate(account.dataAbertura)}</td><td><div className="table-actions"><Link className="table-link" to={`/extracto?numero=${encodeURIComponent(account.numero)}`}>Extracto</Link><button className={`button ${isBlocked ? 'button--outline' : 'button--danger'} button--small`} disabled={busyAccount === account.numero} onClick={() => toggleBlocked(account)}>{busyAccount === account.numero ? 'A processar…' : isBlocked ? 'Desbloquear' : 'Bloquear'}</button></div></td></tr>;
            })}</DataTable>
          </section>}
    </>
  );
}

function Depositos() {
  const [numero, setNumero] = useState('');
  const [account, setAccount] = useState(null);
  const [lookupError, setLookupError] = useState('');
  const [lookupLoading, setLookupLoading] = useState(false);
  const [amount, setAmount] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState(null);
  const toast = useToast();

  async function lookup(event) {
    event?.preventDefault();
    const accountNumber = numero.trim();
    if (!accountNumber) return;
    setLookupLoading(true);
    setLookupError('');
    setAccount(null);
    setReceipt(null);
    try {
      const found = await api.getConta(accountNumber);
      setAccount(found);
    } catch (error) {
      setLookupError(error.message);
    } finally {
      setLookupLoading(false);
    }
  }

  async function deposit(event) {
    event.preventDefault();
    setFormError('');
    const normalizedAmount = amount.trim().replace(',', '.');
    const value = Number(normalizedAmount);
    if (!account || !/^\d+(?:[.,]\d{1,2})?$/.test(amount.trim()) || !Number.isFinite(value) || value <= 0) {
      setFormError('Confirme a conta e indique um valor superior a zero, com no máximo duas casas decimais.');
      return;
    }
    if (String(account.estado).toUpperCase() !== 'ACTIVA') {
      setFormError('Só é possível depositar em contas activas.');
      return;
    }
    if (!window.confirm(`Confirmar depósito de ${formatMoney(value)} na conta ${account.numero}?`)) return;
    setSubmitting(true);
    try {
      const result = await api.depositar(account.numero, value);
      setReceipt({ transaction: result, account });
      setAmount('');
      toast({ message: 'Depósito registado. O recibo está disponível para impressão.' });
    } catch (error) {
      setFormError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  const accountActive = String(account?.estado || '').toUpperCase() === 'ACTIVA';
  return (
    <>
      <PageTitle eyebrow="OPERAÇÕES" title="Depósitos" description="Registe depósitos numa conta existente e imprima o recibo emitido pelo serviço." />
      <div className="two-column">
        <section className="panel"><div className="section-heading"><div><h2>Identificar conta</h2><p>Introduza o número para consultar a conta antes de continuar.</p></div><span className="section-icon" aria-hidden="true">⌕</span></div>
          <form className="search-form search-form--stack" onSubmit={lookup}><Field label="Número da conta" inputMode="numeric" required value={numero} onBlur={() => { if (numero.trim() && account?.numero !== numero.trim()) lookup(); }} onChange={(event) => { setNumero(event.target.value); setAccount(null); setReceipt(null); }} placeholder="Ex.: número da conta" /><button className="button button--outline" type="submit" disabled={lookupLoading || !numero.trim()}>{lookupLoading ? 'A consultar…' : 'Consultar conta'}</button></form>
          {lookupError && <Notice>{lookupError}</Notice>}
          {account && <div className="account-summary"><span className="account-summary__icon">▤</span><div><small>CONTA CONFIRMADA</small><strong className="mono">{account.numero}</strong><span>{account.clienteNome || 'Titular não indicado'}</span><StatusBadge value={account.estado} /></div><div className="account-summary__balance"><small>SALDO ACTUAL</small><strong>{formatMoney(account.saldo)}</strong></div></div>}
        </section>
        <section className="panel"><div className="section-heading"><div><h2>Dados do depósito</h2><p>O depósito será submetido à API documentada.</p></div><span className="section-icon section-icon--orange" aria-hidden="true">＋</span></div>
          {formError && <Notice>{formError}</Notice>}
          <form className="form-grid" onSubmit={deposit}>
            <Field label="Valor (MZN)" type="text" inputMode="decimal" required disabled={!account || !accountActive || submitting} value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0,00" hint="Aceita vírgula ou ponto, com no máximo duas casas decimais." />
            <p className="form-note">Confirme os dados da conta antes de submeter. A operação não pode ser revertida nesta aplicação.</p>
            <div className="form-actions"><button className="button button--primary" type="submit" disabled={!account || !accountActive || submitting}>{submitting ? 'A processar…' : 'Confirmar depósito'}</button></div>
          </form>
        </section>
      </div>
      {receipt && <section className="panel receipt-panel" aria-live="polite"><div className="section-heading"><div><p className="eyebrow">OPERAÇÃO CONCLUÍDA</p><h2>Recibo de depósito</h2></div><button className="button button--outline no-print" onClick={() => window.print()}>Imprimir recibo</button></div>
        <Receipt transaction={receipt.transaction} account={receipt.account} /></section>}
    </>
  );
}

function Receipt({ transaction, account }) {
  return (
    <article className="receipt print-receipt">
      <header className="receipt__header"><span className="brand__mark">B</span><div><strong>{displayBankName(transaction.banco)}</strong><small>Comprovativo de depósito</small></div><StatusBadge value={transaction.estado} /></header>
      <div className="receipt__amount"><small>VALOR DEPOSITADO</small><strong>{formatMoney(transaction.valor)}</strong></div>
      <dl className="details-list details-list--receipt">
        <div><dt>Referência</dt><dd className="mono">{transaction.referencia || '—'}</dd></div>
        <div><dt>Tipo de operação</dt><dd>{transaction.tipo || 'Depósito'}</dd></div>
        <div><dt>Conta</dt><dd className="mono">{transaction.contaDestino || transaction.contaOrigem || account.numero}</dd></div>
        <div><dt>Titular</dt><dd>{account.clienteNome || '—'}</dd></div>
        <div><dt>Data e hora</dt><dd>{formatDate(transaction.dataHora)}</dd></div>
        <div><dt>Registado por</dt><dd>{transaction.registadoPor || '—'}</dd></div>
        <div><dt>Saldo após operação</dt><dd>{formatMoney(transaction.saldoDestino ?? transaction.saldoOrigem)}</dd></div>
      </dl>
      <p className="receipt__foot">Guarde este comprovativo para referência futura.</p>
    </article>
  );
}

function Extracto() {
  const [params, setParams] = useSearchParams();
  const [numero, setNumero] = useState(params.get('numero') || '');
  const [limit, setLimit] = useState('50');
  const [state, setState] = useState({ loading: false, error: '', account: null, movements: null });
  const [searched, setSearched] = useState(false);

  async function load(event) {
    event?.preventDefault();
    const accountNumber = numero.trim();
    const movementLimit = Number(limit);
    if (!accountNumber || !Number.isInteger(movementLimit) || movementLimit < 1 || movementLimit > 100) {
      setState({ loading: false, error: 'Indique uma conta e um limite entre 1 e 100.', account: null, movements: null });
      return;
    }
    setSearched(true);
    setParams({ numero: accountNumber });
    setState({ loading: true, error: '', account: null, movements: null });
    try {
      const [account, movements] = await Promise.all([api.getConta(accountNumber), api.listMovimentos(accountNumber, movementLimit)]);
      setState({ loading: false, error: '', account, movements });
    } catch (error) {
      setState({ loading: false, error: error.message, account: null, movements: null });
    }
  }

  useEffect(() => {
    if (params.get('numero')) load();
  }, []);

  return (
    <>
      <PageTitle eyebrow="CONSULTAS" title="Extracto da conta" description="Consulte os movimentos concluídos de uma conta e imprima o extracto." actions={state.movements && <button className="button button--outline" onClick={() => window.print()}>Imprimir extracto</button>} />
      <section className="panel"><form className="search-form" onSubmit={load}>
        <Field label="Número da conta" inputMode="numeric" required value={numero} onChange={(event) => setNumero(event.target.value)} placeholder="Número da conta" />
        <Field label="Número de movimentos" type="number" min="1" max="100" step="1" required value={limit} onChange={(event) => setLimit(event.target.value)} hint="Entre 1 e 100; por omissão, 50." />
        <button className="button button--primary" type="submit" disabled={state.loading}>{state.loading ? 'A consultar…' : 'Consultar extracto'}</button>
      </form></section>
      {state.error && <Notice title="Não foi possível consultar o extracto" action={<button className="button button--outline button--small" onClick={() => load()}>Tentar novamente</button>}>{state.error}</Notice>}
      {state.loading && <Loading label="A consultar conta e movimentos…" />}
      {state.movements && <section className="panel table-panel print-statement"><div className="section-heading"><div><p className="eyebrow">EXTRACTO DA CONTA</p><h2 className="mono">{state.account.numero}</h2><p>{state.account.clienteNome || 'Titular'} · Saldo actual: {formatMoney(state.account.saldo)}</p></div><span className="result-count">{state.movements.length} movimento(s)</span></div>
        {state.movements.length === 0 ? <EmptyState title="Sem movimentos">O serviço não devolveu movimentos concluídos para esta conta.</EmptyState> :
          <DataTable label="Movimentos da conta" headers={['Data e hora', 'Referência', 'Tipo', 'Contraparte', 'Valor', 'Estado']}>{state.movements.map((item) => {
            const credit = item.sentido === 'CREDITO';
            const counterparty = credit ? item.contaOrigem : item.contaDestino;
            return <tr key={`${item.referencia}-${item.dataHora}`}><td>{formatDate(item.dataHora)}</td><td className="mono">{item.referencia || '—'}</td><td>{item.tipo || '—'}</td><td className="mono">{counterparty || '—'}</td><td><span className={`movement-amount movement-amount--${credit ? 'credit' : 'debit'}`}>{credit ? '+' : '−'}{formatMoney(item.valor)}</span></td><td><StatusBadge value={item.estado} /></td></tr>;
          })}</DataTable>}
        <p className="form-note">Movimentos concluídos devolvidos pelo serviço, do mais recente para o mais antigo.</p>
      </section>}
      {!state.loading && !state.error && !state.movements && !searched && <div className="hint-panel">Introduza o número da conta para consultar os movimentos.</div>}
    </>
  );
}

function Auditoria() {
  const [filters, setFilters] = useState({ ator: '', accao: '', de: '', ate: '', pagina: 0, tamanho: 20 });
  const [state, setState] = useState({ loading: true, error: '', data: null });
  const load = (nextFilters = filters) => {
    setState({ loading: true, error: '', data: null });
    api.listAuditoria({ ator: nextFilters.ator, accao: nextFilters.accao, de: nextFilters.de, ate: nextFilters.ate, pagina: nextFilters.pagina, tamanho: nextFilters.tamanho })
      .then((data) => setState({ loading: false, error: '', data }))
      .catch((error) => setState({ loading: false, error: error.message, data: null }));
  };
  useEffect(() => { load(); }, []);
  function update(key, value) {
    setFilters((current) => ({ ...current, [key]: value, pagina: 0 }));
  }
  function submit(event) {
    event.preventDefault();
    const next = { ...filters, pagina: 0 };
    setFilters(next);
    load(next);
  }
  function changePage(page) {
    const next = { ...filters, pagina: page };
    setFilters(next);
    load(next);
  }
  const data = state.data;
  return (
    <>
      <PageTitle eyebrow="CONTROLO E CONFORMIDADE" title="Auditoria" description="Histórico de acções registadas, com filtros e paginação do serviço." />
      <section className="panel"><form className="audit-filters" onSubmit={submit}>
        <Field label="Actor" value={filters.ator} onChange={(event) => update('ator', event.target.value)} placeholder="Utilizador ou actor" />
        <Field label="Acção" value={filters.accao} onChange={(event) => update('accao', event.target.value)} placeholder="Ex.: LOGIN_FUNCIONARIO" />
        <Field label="Data inicial" type="date" value={filters.de} onChange={(event) => update('de', event.target.value)} />
        <Field label="Data final" type="date" value={filters.ate} onChange={(event) => update('ate', event.target.value)} />
        <label className="field"><span className="field__label">Registos por página</span><select value={filters.tamanho} onChange={(event) => update('tamanho', Number(event.target.value))}><option value="10">10</option><option value="20">20</option><option value="50">50</option><option value="100">100</option><option value="200">200</option></select></label>
        <button className="button button--primary" type="submit" disabled={state.loading}>Aplicar filtros</button>
      </form></section>
      {state.loading ? <section className="panel"><Loading label="A carregar registos de auditoria…" /><div className="skeleton-list" aria-hidden="true"><span /><span /><span /></div></section>
        : state.error ? <Notice title="Erro ao carregar auditoria" action={<button className="button button--outline button--small" onClick={() => load()}>Tentar novamente</button>}>{state.error}</Notice>
          : data?.conteudo?.length === 0 ? <EmptyState title="Sem registos de auditoria">O serviço não devolveu eventos para os filtros seleccionados.</EmptyState>
            : data && <section className="panel table-panel"><div className="section-heading"><div><h2>Registos</h2><p>{Number(data.total || 0).toLocaleString('pt-MZ')} evento(s) no total</p></div><span className="result-count">Página {Number(data.pagina) + 1} de {Math.max(Number(data.totalPaginas) || 1, 1)}</span></div>
              <DataTable label="Registos de auditoria" headers={['Data e hora', 'Tipo de actor', 'Actor', 'Acção', 'Detalhe']}>{data.conteudo.map((event) => <tr key={event.id}><td>{formatDate(event.dataHora)}</td><td><StatusBadge value={event.tipoAtor} /></td><td>{event.ator || '—'}</td><td><span className="action-code">{event.accao || '—'}</span></td><td className="audit-detail">{event.detalhe || '—'}</td></tr>)}</DataTable>
              <div className="pagination"><span>Exibindo {data.conteudo.length} de {Number(data.total || 0)} registos</span><div><button className="button button--outline button--small" onClick={() => changePage(Number(data.pagina) - 1)} disabled={Number(data.pagina) <= 0}>Anterior</button><button className="button button--outline button--small" onClick={() => changePage(Number(data.pagina) + 1)} disabled={Number(data.pagina) + 1 >= Number(data.totalPaginas)}>Seguinte</button></div></div>
            </section>}
    </>
  );
}

function Login() {
  const { session, login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(location.state?.message || '');
  const [loading, setLoading] = useState(false);
  if (session) return <Navigate to="/" replace />;

  async function submit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const response = await api.login({ username: username.trim(), password });
      login(response);
      const destination = location.state?.from?.pathname || '/';
      navigate(destination, { replace: true });
    } catch (err) {
      setError(err.message || 'Não foi possível iniciar sessão.');
    } finally {
      setLoading(false);
      setPassword('');
    }
  }
  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-brand"><span className="brand__mark">B</span><div><strong>{BANK_NAME}</strong><small>Backoffice</small></div></div>
        <p className="eyebrow">ACESSO DE FUNCIONÁRIO</p><h1>Iniciar sessão</h1><p className="page-description">Introduza as suas credenciais de funcionário para continuar.</p>
        {error && <Notice>{error}</Notice>}
        <form className="login-form" onSubmit={submit}>
          <Field label="Utilizador" type="text" autoComplete="username" required value={username} onChange={(event) => setUsername(event.target.value)} placeholder="Nome de utilizador" />
          <Field label="Palavra-passe" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Palavra-passe" />
          <button className="button button--primary button--wide" type="submit" disabled={loading}>{loading ? 'A validar…' : 'Entrar'}</button>
        </form>
        <p className="login-footnote">A sessão é guardada neste dispositivo e termina quando o token expirar.</p>
      </section>
      <div className="login-side"><span className="login-side__mark" aria-hidden="true">◈</span><p>Operações financeiras com clareza e controlo.</p><small>Acesso restrito a funcionários autorizados.</small></div>
    </main>
  );
}

function NotFound() {
  return <section className="not-found"><span className="not-found__code">404</span><h1>Página não encontrada</h1><p>O endereço solicitado não corresponde a uma página do backoffice.</p><Link className="button button--primary" to="/">Voltar à visão geral</Link></section>;
}

export default function App() {
  useEffect(() => {
    document.title = `${BANK_NAME} | Backoffice`;
  }, []);

  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/*" element={<RequireAuth><AppShell /></RequireAuth>} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  );
}
