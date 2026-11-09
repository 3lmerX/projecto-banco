package mz.banco.contas;

import java.math.BigDecimal;

public record TransferenciaInternaResponse(String contaOrigem, String contaDestino,
                                           BigDecimal valor,
                                           BigDecimal saldoOrigem,
                                           BigDecimal saldoDestino) {
}