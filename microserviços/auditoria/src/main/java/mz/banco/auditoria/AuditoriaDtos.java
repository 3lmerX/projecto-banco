package mz.banco.auditoria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

public final class AuditoriaDtos {
    private AuditoriaDtos() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pedido(String tipoAtor, String ator, String accao, String detalhe) {}

    public record AuditoriaResponse(Long id, LocalDateTime dataHora,
                                    String tipoAtor, String ator,
                                    String accao, String detalhe) {
        static AuditoriaResponse de(Auditoria item) {
            return new AuditoriaResponse(item.getId(), item.getDataHora(),
                    item.getTipoAtor(), item.getAtor(), item.getAccao(), item.getDetalhe());
        }
    }

    public record Pagina(java.util.List<AuditoriaResponse> conteudo, int pagina,
                         int tamanho, long total, int totalPaginas) {}
}
