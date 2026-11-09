package mz.banco.contas;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/contas")
public class ContaOperacoesController {

    private final ContaService service;

    public ContaOperacoesController(ContaService service) {
        this.service = service;
    }

    @GetMapping("/{numero}")
    public ContaResponse consultar(@PathVariable("numero") String numero) {
        return service.consultar(numero);
    }

    @PostMapping("/{numero}/depositos")
    public ContaResponse depositar(@PathVariable("numero") String numero,
                                   @RequestBody DepositoRequest req) {
        return service.depositar(numero, req);
    }

    @PostMapping("/transferir")
    public TransferenciaInternaResponse transferir(@RequestBody TransferenciaInternaRequest req) {
        return service.transferir(req);
    }
}