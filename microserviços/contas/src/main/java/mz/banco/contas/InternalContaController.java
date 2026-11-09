package mz.banco.contas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;



@RestController
public class InternalContaController {

    private final ContaService service;

    public InternalContaController(ContaService service) {
        this.service = service;
    }

    @PostMapping("/internal/contas/validar-pin")
    @ResponseStatus(HttpStatus.OK)
    public ValidarPinResponse validarPin(@RequestBody ValidarPinRequest req) {
        return service.validarPin(req.numeroConta(), req.pin());
    }
}