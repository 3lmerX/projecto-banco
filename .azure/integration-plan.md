# Integration hand-off

## Backend
- Existing Maven services: `microserviços/{gateway,autenticacao,contas,transaccoes,auditoria}`; not scaffolded or started in this task.
- Run (per service directory): `./mvnw spring-boot:run` (`mvnw.cmd spring-boot:run` on Windows). Existing service configuration/database credentials are required.
- Gateway base URL: `http://localhost:8080` (from `docs/API.md`). API health endpoint is not specified in the contract; do not invent or probe an undocumented route.
- Read-only live check (2026-11-09): staff login succeeded; `GET /clientes` and `GET /contas` returned 200 with zero records; `GET /auditoria` returned 200 with two records. Credentials and tokens were not recorded. Browser E2E was not attempted.

## Frontend
- Folder: `backoffice/`
- Commands from workspace root: `npm --prefix backoffice run dev`, `npm --prefix backoffice test`, `npm --prefix backoffice run build`
- API seam: `backoffice/src/api/client.js` (native `fetch`, already calls documented live endpoints).
- No mock client, fixtures, fabricated records, duplicate data types, preview-state switcher, or mock files exist. Nothing mock-specific should be deleted or rewired.
- Auth/session: `backoffice/src/auth/session.js`; bearer token remains in memory and expires according to the JWT `exp`. The `ADMIN_EMAIL` principal receives `ROLE_ADMIN`; only that role sees the staff registration page.
- Opening-account PIN exists in memory only while its opening card is open; never persist or log it.

## API route inventory
- `POST /auth/login/funcionario`
- `POST /auth/funcionarios` (Admin principal only)
- `GET /clientes` (optional `q`)
- `GET /clientes/{id}`
- `GET /clientes/{id}/contas`
- `POST /clientes/{id}/contas` (documented account-opening workflow)
- `GET /contas` (optional `q`)
- `GET /contas/{numero}`
- `POST /contas/{numero}/depositos`
- `GET /contas/{numero}/movimentos?limite=1..100`
- `GET /auditoria` (optional `ator`, `accao`, `de`, `ate`, `pagina`, `tamanho`)

No undocumented endpoints are used. `/internal/auditoria` is not part of the gateway contract and is not called.

## Data, migrations, and services
- Database/migration tool/directory/connection variables for this frontend: not applicable; no datastore or schema change is requested.
- **Do not create database migrations or seed data. NO seed data.**
- Shared types package/import alias: none; the frontend is JavaScript and consumes the existing REST contract directly.
- Essential dependency: existing banking API via gateway and its services.
- Enhancement services: none. New Azure resources: none.

## Deferred verification
- Backoffice tests (3) and production build passed; Vite root returned HTTP 200.
- Maven tests passed for all five modules: contas, transaccoes, autenticacao, auditoria, and gateway.
- Authenticated read-only API smoke checks passed for clientes, contas, and auditoria.
- Authenticated browser end-to-end verification remains untested.
