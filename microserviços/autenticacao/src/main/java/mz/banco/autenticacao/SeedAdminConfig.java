package mz.banco.autenticacao;

import mz.banco.autenticacao.model.Funcionario;
import mz.banco.autenticacao.repository.FuncionarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SeedAdminConfig {
    private static final Logger log = LoggerFactory.getLogger(SeedAdminConfig.class);

    @Bean
    ApplicationRunner seedAdmin(FuncionarioRepository repository,
                                @Value("${app.seed.admin-email}") String email,
                                @Value("${app.seed.admin-password}") String password) {
        return args -> {
            if (repository.count() == 0) {
                if (password == null || password.isBlank()) {
                    throw new IllegalStateException(
                            "Defina ADMIN_PASSWORD antes de iniciar o serviço de autenticação");
                }
                repository.save(new Funcionario("Administrador", email,
                        new BCryptPasswordEncoder().encode(password), Funcionario.ACTIVO));
                log.info("Funcionário administrador inicial criado para {}", email);
            }
        };
    }
}
