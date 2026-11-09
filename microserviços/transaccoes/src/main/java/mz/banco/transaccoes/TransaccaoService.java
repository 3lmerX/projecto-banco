package mz.banco.transaccoes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransaccaoService {

    private final TransaccaoRepository repo;
    private final ContasClient contas;
    private final AuditoriaClient auditoria;
    private final String banco;

    public TransaccaoService(TransaccaoRepository repo, ContasClient contas,
                             AuditoriaClient auditoria,
                             @Value("${banco.nome}") String banco) {
        this.repo = repo;
        this.contas = contas;
        this.auditoria = auditoria;
        this.banco = banco;
    }

    public TransaccaoResponse depositar(String numero, DepositoRequest req, String actor) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados do depósito são obrigatórios");
        }
        validarValor(req.valor());

        Transaccao t = registarPendente("DEPOSITO", null, numero, req.valor(), actor);
        try {
            ContasClient.ContaDto conta = contas.depositar(numero, req.valor());
            TransaccaoResponse response = concluir(t, null, conta.saldo());
            auditoria.registar("FUNCIONARIO", actor, "DEPOSITO",
                    "Funcionário " + actor + " depositou " + req.valor() + " na conta " + numero);
            return response;
        } catch (ResponseStatusException e) {
            falhar(t, e.getReason());
            throw e;
        }
    }

    public TransaccaoResponse transferir(TransferenciaRequest req, String actor) {
        if (req == null || req.contaOrigem() == null || req.contaDestino() == null
                || req.pin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conta de origem, conta de destino e PIN são obrigatórios");
        }
        validarValor(req.valor());

        Transaccao t = registarPendente("TRANSFERENCIA",
                req.contaOrigem(), req.contaDestino(), req.valor(), actor);
        try {
            ContasClient.TransferenciaDto r = contas.transferir(
                    req.contaOrigem(), req.contaDestino(), req.valor(), req.pin());
            TransaccaoResponse response = concluir(t, r.saldoOrigem(), r.saldoDestino());
            auditoria.registar("CLIENTE", actor, "TRANSFERENCIA",
                    "Cliente " + actor + " transferiu " + req.valor() + " da conta "
                            + req.contaOrigem() + " para a conta " + req.contaDestino());
            return response;
        } catch (ResponseStatusException e) {
            falhar(t, e.getReason());
            auditoria.registar("CLIENTE", actor, "TRANSFERENCIA_FALHADA",
                    "Cliente " + actor + " tentou transferir da conta " + req.contaOrigem()
                            + " para a conta " + req.contaDestino() + ": " + e.getReason());
            throw e;
        }
    }

    public List<TransaccaoResponse> movimentos(String numero, int limite) {
        if (limite < 1 || limite > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O limite deve estar entre 1 e 100");
        }
        contas.consultar(numero);   // dá 404 se a conta não existir
        return repo.extracto(numero, PageRequest.of(0, limite)).stream()
                .map(t -> TransaccaoResponse.comSaldos(t, null, null, banco,
                        numero.equals(t.getContaOrigem()) ? "DEBITO" : "CREDITO"))
                .toList();
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O valor deve ser maior que zero, com no máximo 2 casas decimais");
        }
    }

    private Transaccao registarPendente(String tipo, String origem,
                                        String destino, BigDecimal valor, String actor) {
        Transaccao t = new Transaccao();
        t.setReferencia("TRX-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase());
        t.setTipo(tipo);
        t.setContaOrigem(origem);
        t.setContaDestino(destino);
        t.setValor(valor);
        t.setEstado(Transaccao.PENDENTE);
        t.setRegistadoPor(actor);
        return repo.save(t);
    }

    private TransaccaoResponse concluir(Transaccao t, BigDecimal saldoOrigem,
                                        BigDecimal saldoDestino) {
        t.setEstado(Transaccao.CONCLUIDA);
        repo.save(t);
        return TransaccaoResponse.comSaldos(t, saldoOrigem, saldoDestino, banco, null);
    }

    private void falhar(Transaccao t, String motivo) {
        t.setEstado(Transaccao.FALHADA);
        t.setMotivo(motivo);
        repo.save(t);
    }
}