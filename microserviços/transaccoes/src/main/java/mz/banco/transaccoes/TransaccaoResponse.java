package mz.banco.transaccoes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccaoResponse(String referencia, String tipo,
                                 String contaOrigem, String contaDestino,
                                 BigDecimal valor, LocalDateTime dataHora,
                                 String estado,
                                 BigDecimal saldoOrigem, BigDecimal saldoDestino,
                                 String sentido, String banco, String registadoPor) {

    public static TransaccaoResponse de(Transaccao t) {
        return comSaldos(t, null, null, null, null);
    }

    public static TransaccaoResponse comSaldos(Transaccao t,
                                               BigDecimal saldoOrigem,
                                               BigDecimal saldoDestino) {
        return comSaldos(t, saldoOrigem, saldoDestino, null, null);
    }

    public static TransaccaoResponse comSaldos(Transaccao t, BigDecimal saldoOrigem,
                                               BigDecimal saldoDestino, String banco,
                                               String sentido) {
        return new TransaccaoResponse(t.getReferencia(), t.getTipo(),
                t.getContaOrigem(), t.getContaDestino(), t.getValor(),
                t.getDataHora(), t.getEstado(), saldoOrigem, saldoDestino,
                sentido, banco, t.getRegistadoPor());
    }
}