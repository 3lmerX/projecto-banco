# Backoffice bancário

Aplicação React + Vite para operações de funcionários, em Português (Moçambique). A aplicação usa exclusivamente a API existente descrita em `../docs/API.md`; não inclui dados de demonstração nem persiste dados bancários no browser.

Para a visão geral do sistema, arquitetura, configuração do backend e testes, consulte o [README principal](../README.md).

## Requisitos

- Node.js 20 ou superior
- npm
- Gateway e serviços da API activos em `http://localhost:8080` para utilizar as páginas com dados reais

## Instalação e execução

Na pasta `backoffice/`:

```sh
npm install
cp .env.example .env
npm run dev
```

O Vite disponibiliza o endereço local no terminal. Para validar:

```sh
npm test
npm run build
```

No PowerShell, copie `.env.example` para `.env` com `Copy-Item .env.example .env`.

## Arrancar o backend no VS Code

Na raiz do projecto, abra **Run and Debug** e seleccione **Banco Digital (todos os serviços)**. A configuração em `.vscode/launch.json` compila e inicia os cinco serviços; os terminais de depuração mostram os respectivos logs:

| Serviço | Porta |
|---|---:|
| Gateway | 8080 |
| Contas | 8081 |
| Transacções | 8082 |
| Autenticação | 8083 |
| Auditoria | 8084 |

O MySQL deve estar activo na porta 3306. Os serviços usam bases de dados separadas por predefinição; por isso, **não defina `DB_URL` no ficheiro partilhado**. Se as credenciais MySQL locais não coincidirem com os valores de desenvolvimento, edite `microserviços/.env.local` (ignorado pelo Git) e defina `DB_USER` e `DB_PASSWORD`. Defina `ADMIN_PASSWORD` apenas se a base de autenticação ainda não tiver o funcionário seed. Não coloque credenciais no código nem as partilhe no chat.

Se o VS Code continuar a apresentar erros de importação Java, execute **Java: Clean Java Language Server Workspace** na Paleta de Comandos e recarregue a janela. A configuração de arranque compila cada módulo antes de o iniciar.

Depois de os cinco serviços estarem activos, mantenha o backoffice em execução com `npm run dev` na pasta `backoffice/` e abra `http://localhost:5173`.

## Configuração

| Variável | Predefinição | Descrição |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | URL base do gateway existente |
| `VITE_BANCO_NOME` | `Banco ISCIM` | Nome mostrado na interface e nos documentos |

O gateway deve permitir pedidos CORS com origem no servidor Vite. Não se configura proxy para criar ou contornar endpoints.

## Sessão e dados sensíveis

- A autenticação usa `POST /auth/login/funcionario`. São aceites `ROLE_FUNCIONARIO` e `ROLE_ADMIN`; somente o Admin principal acessa a gestão de funcionários.
- O token e os dados básicos do funcionário ficam em `localStorage`, conforme o requisito do backoffice; não são guardadas palavras-passe.
- O `exp` do JWT é lido para terminar a sessão quando o token expira. Respostas 401 invalidam a sessão.
- O PIN de abertura só é enviado no pedido documentado de abertura de conta e mantido em memória enquanto o cartão de abertura estiver aberto. Fechar o cartão ou sair da página elimina essa vista; o PIN não é escrito em armazenamento persistente nem apresentado em tabelas, recibos ou registos.
- Não são usados registos sintéticos, clientes de demonstração ou respostas de fallback. Estados sem dados e erros da API são apresentados como tal.

## Páginas e operações REST

| Rota da aplicação | Operações usadas |
|---|---|
| `/login` | `POST /auth/login/funcionario` |
| `/` | `GET /clientes`, `GET /contas` — indicadores calculados apenas das listas recebidas |
| `/clientes` | `GET /clientes?q=…`, `POST /clientes` |
| `/clientes/:id` | `GET /clientes/{id}`, `PUT /clientes/{id}`, `GET /clientes/{id}/contas`; abertura: `POST /clientes/{id}/contas` |
| `/contas` | `GET /contas?q=…`, `PATCH /contas/{numero}/bloquear`, `PATCH /contas/{numero}/desbloquear` |
| `/depositos` | `GET /contas/{numero}`, `POST /contas/{numero}/depositos` |
| `/extracto` | `GET /contas/{numero}`, `GET /contas/{numero}/movimentos?limite=…` |
| `/auditoria` | `GET /auditoria` com filtros documentados e paginação |
| `/funcionarios` (Admin principal) | `GET /auth/funcionarios`, `POST /auth/funcionarios` |

Os cartões, recibos e extractos para impressão usam exclusivamente campos devolvidos pela API e dados introduzidos na operação corrente. O PIN inicial fica apenas em memória enquanto o cartão de abertura está aberto. A pesquisa de extracto aceita limite de 1 a 100; não são enviados filtros de datas não suportados. Não são implementadas rotas internas, transferência ou operações fora do fluxo solicitado.

## Estrutura

- `src/api/client.js`: cliente HTTP nativo (`fetch`) e adaptadores para os endpoints documentados.
- `src/auth/session.js`: sessão persistida em `localStorage` e leitura da expiração do JWT.
- `src/app/App.jsx`: navegação protegida, páginas, formulários, estados de carregamento/erro/vazio e documentos de impressão.
- `src/styles/global.css`: tema responsivo, estados de foco e estilos de impressão.
- `src/test/`: testes com `node:test`, sem dependências de teste adicionais.
