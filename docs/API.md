# API do banco digital

Base URL: `http://localhost:8080`. Excepto login e preflight CORS, os pedidos públicos exigem `Authorization: Bearer <token>`. Valores monetários são números decimais com duas casas. Erros usam o JSON padrão Spring (`timestamp`, `status`, `error`, `message`, `path`).

## Autenticação

### `POST /auth/login/funcionario`

```json
{"username":"admin@banco.mz","password":"<ADMIN_PASSWORD>"}
```

Resposta `200`:

```json
{"token":"<jwt>","tipo":"Bearer","role":"ROLE_ADMIN","nome":"Administrador","primeiroAcesso":null,"banco":"[NOME_DO_BANCO]"}
```

Credenciais inválidas devolvem `401 Credenciais inválidas`; funcionário inactivo devolve `403 Funcionário inactivo`.
Funcionários cujo e-mail não corresponde a `ADMIN_EMAIL` recebem `ROLE_FUNCIONARIO`.

### `GET /auth/funcionarios` (administrador principal)

Lista funcionários por nome. Requer `Authorization: Bearer <jwt>` do administrador principal e devolve apenas `id`, `nome`, `email` e `estado` (nunca a palavra-passe).

Resposta `200`:

```json
[{"id":2,"nome":"Ana M.","email":"ana.funcionaria@banco.mz","estado":"ACTIVO"}]
```

Token ausente ou de outro utilizador devolve `403`.

### `POST /auth/funcionarios` (administrador principal)

Regista um funcionário. Só o utilizador cujo e-mail corresponde a `ADMIN_EMAIL` pode chamar esta rota; o token deve conter `ROLE_ADMIN`. A palavra-passe é armazenada com BCrypt. Funcionários novos recebem `ROLE_FUNCIONARIO` ao iniciar sessão.

Header: `Authorization: Bearer <jwt>`.

Pedido:

```json
{"nome":"Ana M.","email":"ana.funcionaria@banco.mz","palavraPasse":"<INITIAL_PASSWORD>","estado":"ACTIVO"}
```

Resposta `201`:

```json
{"id":2,"nome":"Ana M.","email":"ana.funcionaria@banco.mz","estado":"ACTIVO"}
```

E-mail duplicado devolve `409`; token ausente ou de outro utilizador devolve `403`; dados inválidos devolvem `400`.

### `POST /auth/login/cliente`

```json
{"numeroConta":"1234567890","pin":"1234"}
```

Resposta `200` segue o formato do login anterior com `role: ROLE_CLIENTE`, nome do titular e `primeiroAcesso` booleano. PIN ou conta inválidos devolvem `401 Credenciais inválidas`; conta bloqueada devolve 403.

## Clientes (funcionário)

### `POST /clientes`

Pedido: `{"nome":"Ana M.","numeroBi":"BI123","telefone":"841234567","email":"ana@example.com"}`. Resposta `201`:

```json
{"id":1,"nome":"Ana M.","numeroBi":"BI123","telefone":"841234567","email":"ana@example.com","estado":"ACTIVO"}
```

### `GET /clientes?q=ana`

Pesquisa opcional, parcial e sem distinção entre maiúsculas/minúsculas por nome, BI, telefone e email. Resposta `200`: lista de `ClienteResponse`.

### `GET /clientes/{id}`

Devolve um `ClienteResponse`; `404` quando não existe.

### `PUT /clientes/{id}`

Pedido: `{"nome":"Ana Maria","numeroBi":"BI123","telefone":"841234567","email":"ana@example.com","estado":"ACTIVO"}`. Resposta `200`: `ClienteResponse`. BI duplicado devolve `409 Já existe um cliente com este BI`.

## Contas

### `POST /clientes/{id}/contas` (funcionário)

Pedido: `{"pin":"1234","saldoInicial":500.00}`. Resposta `201`:

```json
{"id":1,"numero":"1234567890","clienteId":1,"clienteNome":"Ana M.","saldo":500.00,"estado":"ACTIVA","dataAbertura":"2026-11-08T12:00:00","primeiroAcesso":true}
```

### `GET /clientes/{id}/contas` (funcionário)

Devolve uma lista de `ContaResponse`.

### `GET /contas?q=ana` (funcionário)

Lista contas, opcionalmente filtradas por número ou nome do titular. Resposta `200`: lista de `ContaResponse`.

### `GET /contas/{numero}`

Funcionário pode consultar qualquer conta; cliente apenas a própria. Resposta `200`: `ContaResponse`; conta inexistente `404`, conta de outro cliente `403`.

### `PATCH /contas/{numero}/bloquear` e `PATCH /contas/{numero}/desbloquear` (funcionário)

Sem corpo. Resposta `200`: `ContaResponse` no estado correspondente. Operações são idempotentes.

### `POST /contas/{numero}/alterar-pin` (cliente titular)

Pedido: `{"pinActual":"1234","pinNovo":"5678"}`. Resposta `204` sem corpo. PIN actual incorrecto `401`; PIN novo inválido/igual ao anterior `400`.

### `POST /contas/{numero}/depositos` (funcionário)

Pedido: `{"valor":250.00}`. Resposta `201` com `TransaccaoResponse`. Saldo inválido ou conta inactiva é rejeitado.

## Transacções

### `POST /transferencias` (cliente)

Pedido: `{"contaOrigem":"1234567890","contaDestino":"9876543210","valor":100.00,"pin":"5678"}`. Resposta `201` com `TransaccaoResponse`. Só a conta do token pode ser origem. PIN inicial por alterar devolve 403; saldo insuficiente ou conta inactiva devolve 409.

`TransaccaoResponse` contém `referencia`, `tipo`, `contaOrigem`, `contaDestino`, `valor`, `dataHora`, `estado`, `saldoOrigem`, `saldoDestino`, `sentido`, `banco` e `registadoPor`. `sentido` só é preenchido no extracto (`DEBITO`/`CREDITO`).

### `GET /contas/{numero}/movimentos?limite=20`

Cliente vê apenas o próprio extracto; funcionário pode consultar qualquer conta. `limite` aceita 1 a 100, por omissão 20. Resposta `200`: lista mais recente primeiro, apenas operações concluídas, com `sentido` relativo à conta consultada.

## Auditoria

### `GET /auditoria?ator=admin&accao=LOGIN_FUNCIONARIO&de=2026-11-01&ate=2026-11-30&pagina=0&tamanho=50` (funcionário)

Filtros são opcionais, datas são inclusivas, ordenação decrescente por data/hora. `tamanho` máximo 200. Exemplo de resposta:

```json
{"conteudo":[{"id":1,"dataHora":"2026-11-08T12:00:00","tipoAtor":"FUNCIONARIO","ator":"admin@banco.mz","accao":"LOGIN_FUNCIONARIO","detalhe":"Funcionário admin@banco.mz iniciou sessão"}],"pagina":0,"tamanho":50,"total":1,"totalPaginas":1}
```

### API interna `POST /internal/auditoria`

Não é encaminhada pelo gateway; requer header `X-Internal-Key`:

```json
{"tipoAtor":"SISTEMA","ator":"servico-autenticacao","accao":"LOGIN_FALHADO","detalhe":"Falha de autenticação para admin@banco.mz"}
```

Resposta `201`: `AuditoriaResponse`.

## Segurança e códigos comuns

- `400`: validação, formato de data ou limite inválido.
- `401`: token, credenciais ou PIN inválidos.
- `403`: papel sem autorização, conta bloqueada ou PIN inicial ainda não alterado.
- `404`: recurso inexistente; o gateway oculta qualquer endpoint `/internal/**`.
- `409`: conflito de negócio (por exemplo, BI duplicado, saldo insuficiente).
- `503`: serviço de Contas indisponível.

Clientes não podem criar depósitos, listar clientes, consultar contas de terceiros nem consultar extractos de terceiros.
