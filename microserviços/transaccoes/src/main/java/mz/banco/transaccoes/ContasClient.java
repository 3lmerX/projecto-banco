package mz.banco.transaccoes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.function.Supplier;
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

    public ContaDto consultar(String numero) {
        return executar(() -> rest.get()
                .uri("/internal/contas/{numero}", numero)
                .retrieve()
                .body(ContaDto.class));
    }

    public ContaDto depositar(String numero, BigDecimal valor) {
        return executar(() -> rest.post()
                .uri("/internal/contas/{numero}/depositos", numero)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DepositoPedido(valor))
                .retrieve()
                .body(ContaDto.class));
    }

    public TransferenciaDto transferir(String origem, String destino,
                                       BigDecimal valor, String pin) {
        return executar(() -> rest.post()
                .uri("/internal/contas/transferir")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TransferenciaPedido(origem, destino, valor, pin))
                .retrieve()
                .body(TransferenciaDto.class));
    }

    // Faz a chamada e converte os erros do serviço de Contas em erros nossos
    private <T> T executar(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode(), mensagem(e));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de Contas indisponível");
        }
    }

    private String mensagem(RestClientResponseException e) {
        try {
            ErroDto erro = e.getResponseBodyAs(ErroDto.class);
            if (erro != null && erro.message() != null && !erro.message().isBlank()) {
                return erro.message();
            }
        } catch (Exception ignorado) {
            // se não der para ler, usa a mensagem genérica
        }
        return "Pedido rejeitado pelo serviço de Contas";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContaDto(String numero, BigDecimal saldo) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransferenciaDto(BigDecimal saldoOrigem, BigDecimal saldoDestino) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErroDto(String message) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepositoPedido(BigDecimal valor) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransferenciaPedido(String contaOrigem, String contaDestino,
                                      BigDecimal valor, String pin) { }
}