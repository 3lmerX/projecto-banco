package mz.banco.auditoria;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @PostMapping("/internal/auditoria")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditoriaDtos.AuditoriaResponse registar(@RequestBody AuditoriaDtos.Pedido pedido) {
        return service.registar(pedido);
    }

    @GetMapping("/auditoria")
    public AuditoriaDtos.Pagina pesquisar(
            @RequestParam(required = false) String ator,
            @RequestParam(required = false) String accao,
            @RequestParam(required = false) LocalDate de,
            @RequestParam(required = false) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return service.pesquisar(ator, accao, de, ate, pagina, tamanho);
    }
}
