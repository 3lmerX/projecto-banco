package mz.banco.contas.security;

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
    private final InternalKeyFilter internalKeyFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          InternalKeyFilter internalKeyFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalKeyFilter = internalKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**").hasAuthority("ROLE_INTERNAL")
                        .requestMatchers("/clientes/**").hasAuthority("ROLE_FUNCIONARIO")
                        .requestMatchers(HttpMethod.GET, "/contas").hasAuthority("ROLE_FUNCIONARIO")
                        .requestMatchers(HttpMethod.PATCH, "/contas/*/bloquear",
                                "/contas/*/desbloquear").hasAuthority("ROLE_FUNCIONARIO")
                        .requestMatchers(HttpMethod.POST, "/contas/*/alterar-pin")
                                .hasAuthority("ROLE_CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/contas/*")
                                .hasAnyAuthority("ROLE_CLIENTE", "ROLE_FUNCIONARIO")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}