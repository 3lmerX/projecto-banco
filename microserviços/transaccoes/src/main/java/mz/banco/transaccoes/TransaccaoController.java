package mz.banco.transaccoes;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TransaccaoController {

    private final TransaccaoService service;

    public TransaccaoController(TransaccaoService service) {
        this.service = service;
    }

    @PostMapping("/contas/{numero}/depositos")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccaoResponse depositar(@PathVariable("numero") String numero,
                                        @RequestBody DepositoRequest req,
                                        Authentication auth) {
        return service.depositar(numero, req, Actor.current().ator());
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccaoResponse transferir(@RequestBody TransferenciaRequest req,
                                         Authentication auth) {
        // A conta de origem é a conta aberta na app
        if (!auth.getName().equals(req.contaOrigem())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Só pode transferir a partir da conta com sessão iniciada");
        }
        return service.transferir(req, Actor.current().ator());
    }

    @GetMapping("/contas/{numero}/movimentos")
    public List<TransaccaoResponse> movimentos(@PathVariable("numero") String numero,
                                               Authentication auth,
                                               @RequestParam(defaultValue = "20") int limite) {
        boolean funcionario = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FUNCIONARIO"));
        if (!funcionario && !auth.getName().equals(numero)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Só pode consultar o extracto da sua conta");
        }
        return service.movimentos(numero, limite);
    }
}