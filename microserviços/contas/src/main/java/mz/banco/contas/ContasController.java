package mz.banco.contas;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/contas")
public class ContasController {
    private final ContaService service;
    private final AuditoriaClient auditoria;

    public ContasController(ContaService service, AuditoriaClient auditoria) {
        this.service = service;
        this.auditoria = auditoria;
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(required = false) String q) {
        return service.pesquisar(q);
    }

    @GetMapping("/{numero}")
    public ContaResponse consultar(@PathVariable String numero, Authentication auth) {
        verificarDono(numero, auth);
        return service.consultar(numero);
    }

    @PatchMapping("/{numero}/bloquear")
    public ContaResponse bloquear(@PathVariable String numero) {
        ContaResponse response = service.bloquear(numero);
        Actor actor = Actor.current();
        auditoria.registar(actor.tipoAtor(), actor.ator(), "BLOQUEAR_CONTA",
                "Funcionário " + actor.ator() + " bloqueou a conta " + numero);
        return response;
    }

    @PatchMapping("/{numero}/desbloquear")
    public ContaResponse desbloquear(@PathVariable String numero) {
        ContaResponse response = service.desbloquear(numero);
        Actor actor = Actor.current();
        auditoria.registar(actor.tipoAtor(), actor.ator(), "DESBLOQUEAR_CONTA",
                "Funcionário " + actor.ator() + " desbloqueou a conta " + numero);
        return response;
    }

    @PostMapping("/{numero}/alterar-pin")
    public ResponseEntity<Void> alterarPin(@PathVariable String numero,
                                           @RequestBody AlterarPinRequest req,
                                           Authentication auth) {
        verificarDono(numero, auth);
        service.alterarPin(numero, req);
        Actor actor = Actor.current();
        auditoria.registar(actor.tipoAtor(), actor.ator(), "ALTERAR_PIN",
                "Cliente " + actor.ator() + " actualizou o PIN da sua conta");
        return ResponseEntity.noContent().build();
    }

    private void verificarDono(String numero, Authentication auth) {
        boolean funcionario = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FUNCIONARIO"));
        if (!funcionario && !auth.getName().equals(numero)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Só pode consultar ou actualizar a sua conta");
        }
    }
}
