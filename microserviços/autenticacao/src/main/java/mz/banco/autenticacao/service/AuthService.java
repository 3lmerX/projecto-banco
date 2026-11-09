package mz.banco.autenticacao.service;

import mz.banco.autenticacao.AuthDtos;
import mz.banco.autenticacao.AuditoriaClient;
import mz.banco.autenticacao.ContasClient;
import mz.banco.autenticacao.JwtUtil;
import mz.banco.autenticacao.model.Funcionario;
import mz.banco.autenticacao.repository.FuncionarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final ContasClient contasClient;
    private final FuncionarioRepository funcionarioRepository;
    private final AuditoriaClient auditoria;
    private final String banco;
    private final String adminEmail;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(JwtUtil jwtUtil, ContasClient contasClient,
                       FuncionarioRepository funcionarioRepository, AuditoriaClient auditoria,
                       @Value("${banco.nome}") String banco,
                       @Value("${app.seed.admin-email}") String adminEmail) {
        this.jwtUtil = jwtUtil;
        this.contasClient = contasClient;
        this.funcionarioRepository = funcionarioRepository;
        this.auditoria = auditoria;
        this.banco = banco;
        this.adminEmail = adminEmail;
    }

    public AuthDtos.TokenResponse autenticarCliente(AuthDtos.LoginClienteRequest req) {
        if (req == null || req.numeroConta() == null || req.pin() == null) {
            String numero = req == null ? null : req.numeroConta();
            auditoria.registar("SISTEMA", identificador(numero), "LOGIN_FALHADO",
                    "Falha de autenticação de cliente para a conta " + identificador(numero));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        ContasClient.ValidarPinResponse conta;
        try {
            conta = contasClient.validarPin(req.numeroConta(), req.pin());
        } catch (ResponseStatusException e) {
            auditoria.registar("SISTEMA", identificador(req.numeroConta()), "LOGIN_FALHADO",
                    "Falha de autenticação de cliente para a conta "
                            + identificador(req.numeroConta()));
            throw e;
        }
        String token = jwtUtil.gerarToken(req.numeroConta(), "ROLE_CLIENTE");
        auditoria.registar("CLIENTE", req.numeroConta(), "LOGIN_CLIENTE",
                "Cliente " + req.numeroConta() + " iniciou sessão");
        return new AuthDtos.TokenResponse(token, "Bearer", "ROLE_CLIENTE",
                conta.clienteNome(), conta.primeiroAcesso(), banco);
    }

    public AuthDtos.TokenResponse autenticarFuncionario(AuthDtos.LoginFuncionarioRequest req) {
        String username = req == null ? null : req.username();
        Funcionario funcionario = username == null || username.isBlank() ? null
                : funcionarioRepository.findByEmail(username).orElse(null);

        if (funcionario == null || req.password() == null
                || !passwordEncoder.matches(req.password(), funcionario.getPalavraPasse())) {
            auditoria.registar("SISTEMA", identificador(username), "LOGIN_FALHADO",
                    "Falha de autenticação de funcionário para o identificador "
                            + identificador(username));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        if (!Funcionario.ACTIVO.equalsIgnoreCase(funcionario.getEstado())) {
            auditoria.registar("SISTEMA", funcionario.getEmail(), "LOGIN_FALHADO",
                    "Tentativa de acesso por funcionário inactivo " + funcionario.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Funcionário inactivo");
        }

        String role = isAdminEmail(funcionario.getEmail()) ? "ROLE_ADMIN" : "ROLE_FUNCIONARIO";
        String token = jwtUtil.gerarToken(funcionario.getEmail(), role);
        auditoria.registar("FUNCIONARIO", funcionario.getEmail(), "LOGIN_FUNCIONARIO",
                "Funcionário " + funcionario.getEmail() + " iniciou sessão");
        return new AuthDtos.TokenResponse(token, "Bearer", role,
                funcionario.getNome(), null, banco);
    }

        public AuthDtos.FuncionarioResponse criarFuncionario(
            String authorization, AuthDtos.CriarFuncionarioRequest request) {
            exigirAdminPrincipal(authorization);

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (funcionarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Já existe um funcionário com este e-mail");
        }

        Funcionario funcionario = new Funcionario(request.nome().trim(), email,
            passwordEncoder.encode(request.palavraPasse()),
            request.estado().trim().toUpperCase(Locale.ROOT));
        Funcionario criado = funcionarioRepository.save(funcionario);
        auditoria.registar("FUNCIONARIO", adminEmail, "FUNCIONARIO_CRIADO",
            "Funcionário " + email + " registado");
        return new AuthDtos.FuncionarioResponse(criado.getId(), criado.getNome(),
            criado.getEmail(), criado.getEstado());
        }

        public List<AuthDtos.FuncionarioResponse> listarFuncionarios(String authorization) {
        exigirAdminPrincipal(authorization);
        return funcionarioRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
            .map(funcionario -> new AuthDtos.FuncionarioResponse(funcionario.getId(),
                funcionario.getNome(), funcionario.getEmail(), funcionario.getEstado()))
            .toList();
        }

        private void exigirAdminPrincipal(String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7) : null;
        if (!jwtUtil.isAdminPrincipal(token, adminEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Apenas o administrador principal pode gerir funcionários");
        }
        }

        private boolean isAdminEmail(String email) {
        return email != null && adminEmail != null && email.equalsIgnoreCase(adminEmail);
        }

    private String identificador(String valor) {
        return valor == null || valor.isBlank() ? "desconhecido" : valor;
    }
}