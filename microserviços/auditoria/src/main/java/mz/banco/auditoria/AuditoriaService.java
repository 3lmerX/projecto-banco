package mz.banco.auditoria;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    public AuditoriaDtos.AuditoriaResponse registar(AuditoriaDtos.Pedido pedido) {
        if (pedido == null || vazio(pedido.tipoAtor()) || vazio(pedido.ator())
                || vazio(pedido.accao())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de actor, actor e acção são obrigatórios");
        }
        if (!List.of("FUNCIONARIO", "CLIENTE", "SISTEMA").contains(pedido.tipoAtor())
                || pedido.ator().length() > 255 || pedido.accao().length() > 100
                || pedido.detalhe() != null && pedido.detalhe().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dados de auditoria inválidos");
        }
        Auditoria evento = new Auditoria();
        evento.setDataHora(java.time.LocalDateTime.now());
        evento.setTipoAtor(pedido.tipoAtor());
        evento.setAtor(pedido.ator());
        evento.setAccao(pedido.accao());
        evento.setDetalhe(pedido.detalhe());
        return AuditoriaDtos.AuditoriaResponse.de(repository.save(evento));
    }

    public AuditoriaDtos.Pagina pesquisar(String ator, String accao, LocalDate de,
                                           LocalDate ate, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Página inválida ou tamanho fora do intervalo de 1 a 200");
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data inicial não pode ser posterior à data final");
        }
        Specification<Auditoria> spec = (root, query, cb) -> cb.conjunction();
        if (ator != null && !ator.isBlank()) {
            String termo = "%" + ator.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("ator")), termo));
        }
        if (accao != null && !accao.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("accao"), accao));
        }
        if (de != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("dataHora"), de.atStartOfDay()));
        }
        if (ate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("dataHora"), ate.plusDays(1).atStartOfDay()));
        }
        Page<Auditoria> result = repository.findAll(spec, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "dataHora").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new AuditoriaDtos.Pagina(result.getContent().stream()
                .map(AuditoriaDtos.AuditoriaResponse::de).toList(), pagina, tamanho,
                result.getTotalElements(), result.getTotalPages());
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
