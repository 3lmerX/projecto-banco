package mz.banco.contas;

public record ClienteRequest(String nome, String numeroBi, String telefone,
                             String email, String estado) {
}
