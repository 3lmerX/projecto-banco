package mz.banco.transaccoes.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Depósito: só o operador (funcionário)
                        .requestMatchers(HttpMethod.POST, "/contas/*/depositos")
                        .hasAuthority("ROLE_FUNCIONARIO")
                        // Transferência: só o cliente
                        .requestMatchers(HttpMethod.POST, "/transferencias")
                        .hasAuthority("ROLE_CLIENTE")
                        // Extracto: cliente ou funcionário (o dono da conta é verificado no controller)
                        .requestMatchers(HttpMethod.GET, "/contas/*/movimentos")
                        .hasAnyAuthority("ROLE_CLIENTE", "ROLE_FUNCIONARIO")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}