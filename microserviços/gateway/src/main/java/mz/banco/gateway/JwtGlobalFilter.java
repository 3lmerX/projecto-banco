package mz.banco.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final Key key;
    private final ObjectMapper objectMapper;

    public JwtGlobalFilter(@Value("${jwt.secret}") String secret, ObjectMapper objectMapper) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Os endpoints internos nunca saem para o exterior
        if (path.startsWith("/internal")) {
            return responder(exchange, HttpStatus.NOT_FOUND);
        }

        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // O login é público
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        // Tudo o resto exige um token válido
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return responder(exchange, HttpStatus.UNAUTHORIZED);
        }
        try {
            Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(header.substring(7));
        } catch (Exception e) {
            return responder(exchange, HttpStatus.UNAUTHORIZED);   // inválido ou expirado
        }
        return chain.filter(exchange);   // o token segue para o serviço, que também o valida
    }

    private Mono<Void> responder(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", status == HttpStatus.NOT_FOUND
                        ? "Recurso não encontrado" : "Token inválido ou ausente",
                "path", exchange.getRequest().getURI().getPath());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}