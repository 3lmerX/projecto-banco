package mz.banco.transaccoes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferenciaRequest(String contaOrigem, String contaDestino,
                                   BigDecimal valor, String pin) {
}