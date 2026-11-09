# Testes de concorrência de Contas

Os testes em `src/test/java/mz/banco/contas/ConcorrenciaTest.java` chamam o `ContaService` directamente e usam H2 isolado em memória através do perfil `test`.

- `doisDepositosSimultaneos`: valida depósitos simultâneos de 100,00 e 50,00 e uma carga de 20 depósitos simultâneos de 10,00, verificando os saldos totais.
- `duasTransferenciasSimultaneasSoUmaPassa`: duas transferências concorrentes tentam gastar o mesmo saldo; exactamente uma deve concluir, a outra deve receber 409 e o saldo não pode ficar negativo.
- `transferenciasCruzadasSemDeadlock`: 20 rondas de transferências simultâneas em direcções opostas; o teste tem timeout de 30 segundos e confirma a conservação do saldo.

Executar em Windows a partir da pasta `contas`:

```powershell
.\mvnw.cmd test
```

Resultado verificado: `BUILD SUCCESS`; 4 testes executados (3 de concorrência e 1 de contexto), sem falhas, erros ou skips.
