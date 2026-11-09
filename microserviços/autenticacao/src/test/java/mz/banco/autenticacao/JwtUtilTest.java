package mz.banco.autenticacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {
    @Test
    void adminPrincipalCheckRequiresTheConfiguredSubjectAndAdminRole() {
        JwtUtil jwtUtil = new JwtUtil("test-secret-with-more-than-thirty-two-characters", 60_000);
        String adminToken = jwtUtil.gerarToken("admin@banco.mz", "ROLE_ADMIN");
        String staffToken = jwtUtil.gerarToken("ana@banco.mz", "ROLE_FUNCIONARIO");
        String wrongRoleToken = jwtUtil.gerarToken("admin@banco.mz", "ROLE_FUNCIONARIO");

        assertTrue(jwtUtil.isAdminPrincipal(adminToken, "admin@banco.mz"));
        assertFalse(jwtUtil.isAdminPrincipal(staffToken, "admin@banco.mz"));
        assertFalse(jwtUtil.isAdminPrincipal(wrongRoleToken, "admin@banco.mz"));
        assertFalse(jwtUtil.isAdminPrincipal("invalid-token", "admin@banco.mz"));
    }
}