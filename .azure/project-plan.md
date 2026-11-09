# Project Plan

**Status**: Awaiting Integration
**Created**: 2026-11-08
**Mode**: AUGMENT

---

## 1. Project Overview

**Goal**: Create a JavaScript React + Vite bank backoffice under `backoffice/`, in Portuguese (Moçambique), branded `[NOME_DO_BANCO]`, consuming only the existing REST contract in `docs/API.md`. Include staff login, dashboard, client search/detail, accounts, deposits with printable receipts, account statements, and audit history. Use the documented endpoints and response fields as the source of truth; do not generate fictional bank records. The project is designed so that every module is independently testable.

**App Type**: Static + API

**API Login**: Yes

**Mode**: AUGMENT

**Deployment Plan**: No deployment plan found

---

## 2. Frontend — Web App

| Component | Technology |
|-----------|-----------|
| **Language** | JavaScript |
| **Framework** | React + Vite |
| **Package Manager** | npm |
| **Test Runner** | Node.js built-in `node:test` (no additional test dependency) |
| **Mocking Library** | None required |
| **Test Command** | `node --test` |

Runtime dependencies are limited to `react`, `react-dom`, and `react-router-dom`. Vite and its React plugin are development/build tooling; do not add a component, icon, HTTP, or test library. Use native `fetch`, CSS, browser print, and accessible HTML controls.

---

## 3. Services Required

| Azure Service | Role in App | Environment Variable | Default Value (Local) | Classification |
|---------------|------------|---------------------|----------------------|----------------|
| None | Static SPA consumes the existing gateway/API; no new Azure resource or datastore is requested. | `VITE_API_URL`, `VITE_BANCO_NOME` | `http://localhost:8080`, `[NOME_DO_BANCO]` | Not applicable |

---

## 4. Prerequisites

### Run

| Tool | Service(s) | Installed | Version |
|------|------------|-----------|---------|
| Node.js | backoffice | ✅ | v24.21.0 |
| npm | backoffice | ✅ | 11.19.0 |

### Debug

| Tool | Service(s) | Installed | Version |
|------|------------|-----------|---------|
| Visual Studio Code (optional editor/debug host) | backoffice | ✅ | 1.144.1 |
| Browser with JavaScript debugging (Edge/Chrome) | backoffice | ❓ | Not confirmed |

Double-check any tool marked ❓ before development. No Docker or Azure tooling is required for this frontend-only project.

---

## 5. Design System & UI

**Component Library**: Native accessible HTML + CSS; Fluent UI v9-inspired visual language without adding a component-library dependency
**Style Direction**: A calm, trustworthy banking operations console with deep navy primary actions, restrained teal highlights, readable account tables, clear transaction states, and subtle elevation. Keep the responsive interface keyboard-accessible and optimise for scanning real service data.
**Typography**: system-ui, Segoe UI, sans-serif

### Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `primary` | `#123B63` | Primary navigation and actions |
| `accent` | `#1B8A88` | Secondary emphasis and positive states |
| `surface` | `#F4F7FA` | Backoffice page background |
| `text` | `#1A2633` | Main interface text |
| `muted` | `#637386` | Supporting labels and timestamps |
| `border` | `#D8E1EA` | Table, card, and form boundaries |

### Pages

| Page | Route | Purpose | Layout |
|------|-------|---------|--------|
| Dashboard | `/` | Show client count, account count, and sum of account balances calculated from live documented list endpoints; no synthetic metrics or fallback records. | `header, sidebar, grid, table` |
| Clientes | `/clientes` | Search clients using optional `q` and show only records returned by `GET /clientes`. | `header, sidebar, actions, table, empty` |
| Detalhe do cliente | `/clientes/:id` | Load a specific client and their accounts using `GET /clientes/{id}` and `GET /clientes/{id}/contas`. | `header, sidebar, tabs, table` |
| Contas | `/contas` | Search accounts using optional `q` and show balances/status from `GET /contas`. | `header, sidebar, actions, table, error` |
| Depósitos | `/depositos` | Select/identify an account and submit its documented deposit operation; show returned transaction as a printable receipt. | `header, sidebar, form, action-bar` |
| Extracto da conta | `/contas/:numero/movimentos` | Show recent movements for the selected account using `GET /contas/{numero}/movimentos?limite=20`; allow a `limite` from 1 to 100 and print. No date-range filter. | `header, sidebar, form, table` |
| Auditoria | `/auditoria` | Filter and display records from the documented audit endpoint, including pagination. | `header, sidebar, actions, table, loading (skeleton)` |
| Iniciar sessão | `/login` | Authenticate a staff member through the documented employee login endpoint. | `form, actions` |

### Sample Content

No example customer, account, transaction, audit, or dashboard metric records are fabricated. Every data row, account balance, count, sum, receipt, and audit event shown by the application must come from a successful API response. Before data is returned, present loading, empty, or error states without substituting sample entities.

Dashboard — live aggregations only:
- Client count: number of records returned by `GET /clientes`.
- Account count and total balance: count and sum of `saldo` from `GET /contas`.
- Recent activity: only transaction/audit responses from documented endpoints; do not invent entries.

Deposits — form fields follow the contract for `POST /contas/{numero}/depositos`: account number and `valor`. Receipt fields are taken from the returned `TransaccaoResponse`.

Extracto — movements are loaded from `GET /contas/{numero}/movimentos?limite=20` (limit 1–100); use only returned `TransaccaoResponse` fields.

Auditoria — rows and pagination are taken from `GET /auditoria` response fields `conteudo`, `pagina`, `tamanho`, `total`, and `totalPaginas`.

---

## 6. Project Structure

```text
projecto-banco/
├── docs/
│   └── API.md                         # Authoritative existing REST contract
└── backoffice/
    ├── .env.example                   # VITE_API_URL, VITE_BANCO_NOME
    ├── README.md                     # Setup, run/build, configuration, API notes
    ├── package.json                  # React, React DOM, React Router; Vite tooling
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.jsx
        ├── app/
        │   ├── App.jsx
        │   └── routes.jsx
        ├── api/                       # Native fetch client and documented endpoint adapters
        ├── auth/                      # Employee login and session state
        ├── components/                # Shared accessible controls and responsive layout
        ├── features/
        │   ├── dashboard/             # Counts and balance sum from live API responses
        │   ├── clientes/
        │   ├── contas/
        │   ├── depositos/
        │   ├── extractos/
        │   └── auditoria/
        ├── styles/                    # Responsive theme and print styles
        └── test/                      # Node built-in tests; no third-party test package
```

---

## 7. Route Definitions

These are client-side routes and the REST operations they invoke; use the documented gateway endpoints and methods exactly.

| # | Method | Path | Description | Request Body | Response Body | Status Codes |
|---|--------|------|-------------|-------------|--------------|-------------|
| 1 | POST | `/auth/login/funcionario` | Staff login from `/login`. | `{"username":"…","password":"…"}` | `{token,tipo,role,nome,primeiroAcesso,banco}` | 200, 401, 403 |
| 2 | GET | `/clientes` | List/search for dashboard count and client directory; optional `q`. | — | List of `ClienteResponse` | 200 |
| 3 | GET | `/clientes/:id` | Client detail screen. | — | `ClienteResponse` | 200, 404 |
| 4 | GET | `/clientes/:id/contas` | Accounts for the selected client. | — | List of `ContaResponse` | 200 |
| 5 | GET | `/contas` | Account directory and dashboard account count/balance sum; optional `q`. | — | List of `ContaResponse` | 200 |
| 6 | GET | `/contas/:numero` | Account detail and account lookup. | — | `ContaResponse` | 200, 403, 404 |
| 7 | POST | `/contas/:numero/depositos` | Deposit form at client route `/depositos`; print returned transaction receipt. | `{"valor": number}` | `TransaccaoResponse` | 201; documented validation/auth errors |
| 8 | GET | `/contas/:numero/movimentos?limite=20` | Account statement; `limite` is 1–100, default 20. Do not send unsupported date-range filters. | — | List of completed `TransaccaoResponse` movements | 200; documented validation/auth errors |
| 9 | GET | `/auditoria` | Audit list; support documented optional `ator`, `accao`, `de`, `ate`, `pagina`, and `tamanho` filters. | — | `{conteudo,pagina,tamanho,total,totalPaginas}` | 200, 400, 401, 403 |

---

## 8. Next Steps

1. Run **azure-project-scaffold** only after explicit plan approval.
2. Implement against `docs/API.md`; do not add fictional API data, new backend endpoints, or a datastore.
3. Keep runtime dependencies to `react`, `react-dom`, and `react-router-dom`; use Vite only for build/development.
4. Validate with `npm install`, `npm run dev`, and `npm run build`; document configuration and run commands in `backoffice/README.md`.
