package mz.banco.autenticacao.service;

import mz.banco.autenticacao.AuditoriaClient;
import mz.banco.autenticacao.AuthDtos;
import mz.banco.autenticacao.ContasClient;
import mz.banco.autenticacao.JwtUtil;
import mz.banco.autenticacao.model.Funcionario;
import mz.banco.autenticacao.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final FuncionarioRepository repository = mock(FuncionarioRepository.class);
    private final AuditoriaClient auditoria = mock(AuditoriaClient.class);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(jwtUtil, mock(ContasClient.class), repository,
                auditoria, "Banco ISCIM", "admin@banco.mz");
    }

    @Test
    void principalAdminReceivesAdminRoleAtLogin() {
        repositoryReturns("admin@banco.mz", "Administrador");
        when(jwtUtil.gerarToken("admin@banco.mz", "ROLE_ADMIN")).thenReturn("admin-token");

        AuthDtos.TokenResponse response = service.autenticarFuncionario(
                new AuthDtos.LoginFuncionarioRequest("admin@banco.mz", "admin12345"));

        assertEquals("ROLE_ADMIN", response.role());
    }

    @Test
    void regularStaffReceivesStaffRoleAtLogin() {
        repositoryReturns("ana@banco.mz", "Ana");
        when(jwtUtil.gerarToken("ana@banco.mz", "ROLE_FUNCIONARIO")).thenReturn("staff-token");

        AuthDtos.TokenResponse response = service.autenticarFuncionario(
                new AuthDtos.LoginFuncionarioRequest("ana@banco.mz", "admin12345"));

        assertEquals("ROLE_FUNCIONARIO", response.role());
    }

    @Test
    void registrationStoresOnlyEncodedPasswordAndReturnsSafeFields() {
        when(jwtUtil.isAdminPrincipal("admin-token", "admin@banco.mz")).thenReturn(true);
        when(repository.existsByEmailIgnoreCase("ana@banco.mz")).thenReturn(false);
        when(repository.save(any(Funcionario.class))).thenAnswer(invocation -> {
            Funcionario funcionario = invocation.getArgument(0);
            funcionario.setId(12L);
            return funcionario;
        });

        AuthDtos.FuncionarioResponse response = service.criarFuncionario("Bearer admin-token",
                new AuthDtos.CriarFuncionarioRequest("Ana", "ANA@BANCO.MZ",
                        "senha-inicial", Funcionario.ACTIVO));

        assertEquals(12L, response.id());
        assertEquals("ana@banco.mz", response.email());
        verify(repository).save(argThat(funcionario ->
                new BCryptPasswordEncoder().matches("senha-inicial", funcionario.getPalavraPasse())));
    }

    @Test
    void nonAdminTokenCannotRegisterStaff() {
        when(jwtUtil.isAdminPrincipal("staff-token", "admin@banco.mz")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criarFuncionario("Bearer staff-token",
                        new AuthDtos.CriarFuncionarioRequest("Ana", "ana@banco.mz",
                                "senha-inicial", Funcionario.ACTIVO)));

        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test
    void duplicateEmailCannotBeRegistered() {
        when(jwtUtil.isAdminPrincipal("admin-token", "admin@banco.mz")).thenReturn(true);
        when(repository.existsByEmailIgnoreCase("ana@banco.mz")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.criarFuncionario("Bearer admin-token",
                        new AuthDtos.CriarFuncionarioRequest("Ana", "ana@banco.mz",
                                "senha-inicial", Funcionario.ACTIVO)));

        assertEquals(409, exception.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void adminCanListEmployeesWithoutPasswordFields() {
        when(jwtUtil.isAdminPrincipal("admin-token", "admin@banco.mz")).thenReturn(true);
        Funcionario funcionario = new Funcionario("Ana", "ana@banco.mz",
                "bcrypt-hash", Funcionario.ACTIVO);
        funcionario.setId(12L);
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(funcionario));

        List<AuthDtos.FuncionarioResponse> result = service.listarFuncionarios("Bearer admin-token");

        assertEquals(1, result.size());
        assertEquals(new AuthDtos.FuncionarioResponse(12L, "Ana", "ana@banco.mz",
                Funcionario.ACTIVO), result.get(0));
    }

    @Test
    void nonAdminCannotListEmployees() {
        when(jwtUtil.isAdminPrincipal("staff-token", "admin@banco.mz")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.listarFuncionarios("Bearer staff-token"));

        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    private void repositoryReturns(String email, String name) {
        Funcionario funcionario = new Funcionario(name, email,
                new BCryptPasswordEncoder().encode("admin12345"), Funcionario.ACTIVO);
        when(repository.findByEmail(email)).thenReturn(Optional.of(funcionario));
    }
}