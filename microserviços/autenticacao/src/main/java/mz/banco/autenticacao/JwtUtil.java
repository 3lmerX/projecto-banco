package mz.banco.autenticacao;


// Geração de Tokens

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String gerarToken(String subject, String role) {
        Date agora = new Date();
        Date validade = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(agora)
                .setExpiration(validade)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isAdminPrincipal(String token, String adminEmail) {
        if (token == null || adminEmail == null || adminEmail.isBlank()) return false;
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            return adminEmail.equalsIgnoreCase(claims.getSubject())
                    && "ROLE_ADMIN".equals(claims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}