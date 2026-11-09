package mz.banco.contas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContaResponse(Long id, String numero, Long clienteId,
                            String clienteNome,
                            BigDecimal saldo, String estado,
                            LocalDateTime dataAbertura, boolean primeiroAcesso) {

    public static ContaResponse de(Conta c) {
        return new ContaResponse(c.getId(), c.getNumero(), c.getCliente().getId(),
                c.getCliente().getNome(), c.getSaldo(), c.getEstado(),
                c.getDataAbertura(), c.isPrimeiroAcesso());
    }
}