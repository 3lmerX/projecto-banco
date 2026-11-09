package mz.banco.contas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferenciaInternaRequest(String contaOrigem, String contaDestino,
                                          BigDecimal valor, String pin) {
}