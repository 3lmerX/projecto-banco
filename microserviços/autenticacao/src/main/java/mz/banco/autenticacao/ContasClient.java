package mz.banco.autenticacao;

//Validação do PIN da Conta

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ContasClient {

    private final RestClient rest;

    public ContasClient(@Value("${contas.url}") String contasUrl,
                        @Value("${internal.api-key}") String chaveInterna) {
        this.rest = RestClient.builder()
                .baseUrl(contasUrl)
                .defaultHeader("X-Internal-Key", chaveInterna)
                .build();
    }

    public ValidarPinResponse validarPin(String numeroConta, String pin) {
        try {
            return rest.post()
                    .uri("/internal/contas/validar-pin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ValidarPinRequest(numeroConta, pin))
                    .retrieve()
                    .body(ValidarPinResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta bloqueada");
            }
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
            }
            throw new ResponseStatusException(e.getStatusCode(), "Serviço de Contas rejeitou o pedido");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de Contas indisponível");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidarPinResponse(String numeroConta, String clienteNome,
                                    boolean primeiroAcesso, String estado) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidarPinRequest(String numeroConta, String pin) {}
}