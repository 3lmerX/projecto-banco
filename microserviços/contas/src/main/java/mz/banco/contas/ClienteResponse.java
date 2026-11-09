package mz.banco.contas;

public record ClienteResponse(Long id, String nome, String numeroBi,
                              String telefone, String email, String estado) {
    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(),
                cliente.getNumeroBi(), cliente.getTelefone(),
                cliente.getEmail(), cliente.getEstado());
    }
}
