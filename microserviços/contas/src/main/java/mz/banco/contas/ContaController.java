package mz.banco.contas;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes/{clienteId}/contas")
public class ContaController {

    private final ContaService service;
    private final AuditoriaClient auditoria;

    public ContaController(ContaService service, AuditoriaClient auditoria) {
        this.service = service;
        this.auditoria = auditoria;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse abrir(@PathVariable("clienteId") Long clienteId,
                               @RequestBody AbrirContaRequest req) {
        ContaResponse response = service.abrir(clienteId, req);
        Actor actor = Actor.current();
        auditoria.registar(actor.tipoAtor(), actor.ator(), "ABRIR_CONTA",
                "Funcionário " + actor.ator() + " criou a conta " + response.numero()
                        + " do cliente " + clienteId + " com saldo inicial " + response.saldo());
        return response;
    }

    @GetMapping
    public List<ContaResponse> listar(@PathVariable("clienteId") Long clienteId) {
        return service.listar(clienteId);
    }
}