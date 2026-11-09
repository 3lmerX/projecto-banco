package mz.banco.autenticacao;

//endpoints
import mz.banco.autenticacao.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/cliente")
    public ResponseEntity<AuthDtos.TokenResponse> loginCliente(@RequestBody AuthDtos.LoginClienteRequest request) {
        AuthDtos.TokenResponse response = authService.autenticarCliente(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/funcionario")
    public ResponseEntity<AuthDtos.TokenResponse> loginFuncionario(@RequestBody AuthDtos.LoginFuncionarioRequest request) {
        AuthDtos.TokenResponse response = authService.autenticarFuncionario(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/funcionarios")
    public ResponseEntity<AuthDtos.FuncionarioResponse> criarFuncionario(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AuthDtos.CriarFuncionarioRequest request) {
        AuthDtos.FuncionarioResponse response = authService.criarFuncionario(authorization, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/funcionarios")
    public ResponseEntity<List<AuthDtos.FuncionarioResponse>> listarFuncionarios(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ResponseEntity.ok(authService.listarFuncionarios(authorization));
    }
}