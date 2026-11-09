package mz.banco.auditoria.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && jwtUtil.validarToken(header.substring(7))) {
            Claims claims = jwtUtil.extrairClaims(header.substring(7));
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
                if (subject != null && ("ROLE_FUNCIONARIO".equals(role)
                    || "ROLE_ADMIN".equals(role))) {
                var authorities = "ROLE_ADMIN".equals(role)
                    ? List.of(new SimpleGrantedAuthority(role),
                        new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))
                    : List.of(new SimpleGrantedAuthority(role));
                var auth = new UsernamePasswordAuthenticationToken(subject, null,
                    authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
