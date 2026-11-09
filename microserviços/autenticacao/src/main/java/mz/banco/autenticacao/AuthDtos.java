package mz.banco.autenticacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginClienteRequest(String numeroConta, String pin) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginFuncionarioRequest(String username, String password) {}
        public record CriarFuncionarioRequest(
            @NotBlank @Size(max = 160) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String palavraPasse,
            @NotBlank @Pattern(regexp = "(?i)ACTIVO|BLOQUEADO") String estado) {}
        public record FuncionarioResponse(Long id, String nome, String email, String estado) {}
    public record TokenResponse(String token, String tipo, String role, String nome,
                                Boolean primeiroAcesso, String banco) {}
}