package mz.banco.contas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbrirContaRequest(String pin, BigDecimal saldoInicial) {
}