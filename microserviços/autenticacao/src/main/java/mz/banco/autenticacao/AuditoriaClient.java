package mz.banco.autenticacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuditoriaClient {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaClient.class);
    private final RestClient rest;

    public AuditoriaClient(@Value("${auditoria.url}") String url,
                           @Value("${internal.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.rest = RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeader("X-Internal-Key", apiKey).build();
    }

    public void registar(String tipoAtor, String ator, String accao, String detalhe) {
        try {
            rest.post().uri("/internal/auditoria").body(new Pedido(tipoAtor, ator, accao, detalhe))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Não foi possível registar a acção de auditoria {}", accao, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pedido(String tipoAtor, String ator, String accao, String detalhe) {
    }
}
