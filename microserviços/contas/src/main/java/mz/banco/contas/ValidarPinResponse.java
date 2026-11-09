package mz.banco.contas;

public record ValidarPinResponse(String numeroConta, String clienteNome,
                                 boolean primeiroAcesso, String estado) {
}
