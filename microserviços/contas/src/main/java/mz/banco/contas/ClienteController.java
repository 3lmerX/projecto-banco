package mz.banco.contas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository repo;
    private final AuditoriaClient auditoria;

    public ClienteController(ClienteRepository repo, AuditoriaClient auditoria){
        this.repo = repo;
        this.auditoria = auditoria;
    }

 @GetMapping
 public List<ClienteResponse> listar(@RequestParam(required = false) String q) {
        List<Cliente> clientes = q == null || q.isBlank()
                ? repo.findAll() : repo.pesquisar(q.trim());
        return clientes.stream().map(ClienteResponse::de).toList();
 }

 @PostMapping
 @ResponseStatus(HttpStatus.CREATED)
 public ClienteResponse criar(@RequestBody ClienteRequest req) {
    validar(req, false);
    if (repo.existsByNumeroBi(req.numeroBi().trim())) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "Já existe um cliente com este BI");
    }
    Cliente cliente = aplicar(new Cliente(), req);
    cliente.setEstado(Cliente.ACTIVO);
    ClienteResponse response = ClienteResponse.de(repo.save(cliente));
    Actor actor = Actor.current();
    auditoria.registar(actor.tipoAtor(), actor.ator(), "CRIAR_CLIENTE",
            "Funcionário " + actor.ator() + " criou o cliente " + response.id());
    return response;
 }

 @GetMapping("/{id}")
 public ClienteResponse consultar(@PathVariable Long id) {
    return ClienteResponse.de(repo.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cliente não encontrado")));
 }

 @PutMapping("/{id}")
 public ClienteResponse actualizar(@PathVariable Long id,
                                    @RequestBody ClienteRequest req) {
    validar(req, true);
    Cliente cliente = repo.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    if (repo.existsByNumeroBiAndIdNot(req.numeroBi().trim(), id)) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "Já existe um cliente com este BI");
    }
    aplicar(cliente, req);
    ClienteResponse response = ClienteResponse.de(repo.save(cliente));
    Actor actor = Actor.current();
    auditoria.registar(actor.tipoAtor(), actor.ator(), "EDITAR_CLIENTE",
            "Funcionário " + actor.ator() + " actualizou o cliente " + id);
    return response;
 }

 private Cliente aplicar(Cliente cliente, ClienteRequest req) {
    cliente.setNome(req.nome().trim());
    cliente.setNumeroBi(req.numeroBi().trim());
    cliente.setTelefone(req.telefone().trim());
    cliente.setEmail(req.email() == null || req.email().isBlank()
            ? null : req.email().trim());
    if (req.estado() != null) cliente.setEstado(req.estado().trim().toUpperCase());
    return cliente;
 }

 private void validar(ClienteRequest req, boolean actualizar) {
    if (req == null || req.nome() == null || req.nome().isBlank()
            || req.numeroBi() == null || req.numeroBi().isBlank()
            || req.telefone() == null || req.telefone().isBlank()) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Nome, número do BI e telefone são obrigatórios");
    }
    if (req.email() != null && !req.email().isBlank()
            && !req.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Email inválido");
    }
    if (actualizar && (req.estado() == null
            || !List.of(Cliente.ACTIVO, Cliente.INACTIVO)
                    .contains(req.estado().trim().toUpperCase()))) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Estado do cliente inválido");
    }
 }
}
