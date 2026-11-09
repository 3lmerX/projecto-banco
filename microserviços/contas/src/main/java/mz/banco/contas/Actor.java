package mz.banco.contas;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public record Actor(String tipoAtor, String ator) {
    public static Actor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Não existe identidade autenticada no contexto");
        }
        String tipoAtor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FUNCIONARIO")) ? "FUNCIONARIO"
                : authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")) ? "CLIENTE" : null;
        if (tipoAtor == null) {
            throw new IllegalStateException("O papel autenticado não pode ser auditado");
        }
        return new Actor(tipoAtor, authentication.getName());
    }
}
