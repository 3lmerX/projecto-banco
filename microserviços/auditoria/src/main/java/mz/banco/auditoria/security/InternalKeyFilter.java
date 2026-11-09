package mz.banco.auditoria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalKeyFilter extends OncePerRequestFilter {
    private final byte[] key;

    public InternalKeyFilter(@Value("${internal.api-key}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/")) {
            String received = request.getHeader("X-Internal-Key");
            if (received != null && MessageDigest.isEqual(
                    received.getBytes(StandardCharsets.UTF_8), key)) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("servico-interno", null,
                                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
            }
        }
        chain.doFilter(request, response);
    }
}
