package mz.banco.contas;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaService {

    private final ContaRepository contaRepo;
    private final ClienteRepository clienteRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ContaService(ContaRepository contaRepo, ClienteRepository clienteRepo) {
        this.contaRepo = contaRepo;
        this.clienteRepo = clienteRepo;
    }

    public ContaResponse abrir(Long clienteId, AbrirContaRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados da conta são obrigatórios");
        }
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        if (req.pin() == null || !req.pin().matches("\\d{4}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O PIN deve ter exactamente 4 dígitos");
        }

        BigDecimal saldo = req.saldoInicial() == null ? BigDecimal.ZERO : req.saldoInicial();
        if (saldo.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O saldo inicial não pode ser negativo");
        }
        if (saldo.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O saldo inicial aceita no máximo 2 casas decimais");
        }

        Conta conta = new Conta();
        conta.setNumero(gerarNumero());
        conta.setCliente(cliente);
        conta.setPinHash(encoder.encode(req.pin()));
        conta.setSaldo(saldo);

        return ContaResponse.de(contaRepo.save(conta));
    }

    public List<ContaResponse> listar(Long clienteId) {
        if (!clienteRepo.existsById(clienteId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cliente não encontrado");
        }
        return contaRepo.findByClienteId(clienteId).stream()
                .map(ContaResponse::de)
                .toList();
    }

    public ContaResponse consultar(String numero) {
        return contaRepo.findByNumero(numero)
                .map(ContaResponse::de)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    @Transactional
    public ContaResponse depositar(String numero, DepositoRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados do depósito são obrigatórios");
        }
        BigDecimal valor = req.valor();
        if (valor == null || valor.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O valor deve ser maior que zero");
        }
        if (valor.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O valor aceita no máximo 2 casas decimais");
        }

        Conta conta = contaRepo.buscarParaAlterar(numero)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (!Conta.ACTIVA.equals(conta.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A conta não está activa");
        }

        conta.setSaldo(conta.getSaldo().add(valor));
        return ContaResponse.de(conta);
    }

    private String gerarNumero() {
        String numero;
        do {
            numero = String.valueOf(1_000_000_000L + (long) (Math.random() * 9_000_000_000L));
        } while (contaRepo.existsByNumero(numero));
        return numero;
    }

    @Transactional
    public TransferenciaInternaResponse transferir(TransferenciaInternaRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados da transferência são obrigatórios");
        }
        String numOrigem = req.contaOrigem();
        String numDestino = req.contaDestino();
        BigDecimal valor = req.valor();

        if (numOrigem == null || numDestino == null || req.pin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conta de origem, conta de destino e PIN são obrigatórios");
        }
        if (numOrigem.equals(numDestino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "As contas de origem e destino devem ser diferentes");
        }
        if (valor == null || valor.signum() <= 0 || valor.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O valor deve ser maior que zero, com no máximo 2 casas decimais");
        }

        // Bloquear SEMPRE primeiro a conta com o número menor (evita bloqueio mútuo)
        String primeiro = numOrigem.compareTo(numDestino) < 0 ? numOrigem : numDestino;
        String segundo = primeiro.equals(numOrigem) ? numDestino : numOrigem;

        Conta c1 = buscarParaAlterar(primeiro);
        Conta c2 = buscarParaAlterar(segundo);
        Conta origem = c1.getNumero().equals(numOrigem) ? c1 : c2;
        Conta destino = (origem == c1) ? c2 : c1;

        if (!Conta.ACTIVA.equals(origem.getEstado()) || !Conta.ACTIVA.equals(destino.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "As duas contas têm de estar activas");
        }
        if (!pinCorrecto(req.pin(), origem)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PIN incorrecto");
        }
        if (origem.isPrimeiroAcesso()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Deve alterar o PIN inicial antes de efectuar transferências");
        }
        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente");
        }

        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));

        return new TransferenciaInternaResponse(numOrigem, numDestino, valor,
                origem.getSaldo(), destino.getSaldo());
    }

    private Conta buscarParaAlterar(String numero) {
        return contaRepo.buscarParaAlterar(numero)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada: " + numero));
    }

    private boolean pinCorrecto(String pin, Conta conta) {
        return encoder.matches(pin, conta.getPinHash());

    }

    public ValidarPinResponse validarPin(String numeroConta, String pin) {
        if (numeroConta == null || pin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Credenciais inválidas");
        }
        Conta conta = contaRepo.findByNumero(numeroConta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        if (pin == null || !encoder.matches(pin, conta.getPinHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        if (!Conta.ACTIVA.equals(conta.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta bloqueada");
        }
        return new ValidarPinResponse(conta.getNumero(), conta.getCliente().getNome(),
                conta.isPrimeiroAcesso(), conta.getEstado());
    }

    public List<ContaResponse> pesquisar(String q) {
        List<Conta> contas = q == null || q.isBlank()
                ? contaRepo.findAll() : contaRepo.pesquisar(q.trim());
        return contas.stream().map(ContaResponse::de).toList();
    }

    public ContaResponse bloquear(String numero) {
        Conta conta = contaRepo.findByNumero(numero).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        conta.setEstado(Conta.BLOQUEADA);
        return ContaResponse.de(contaRepo.save(conta));
    }

    public ContaResponse desbloquear(String numero) {
        Conta conta = contaRepo.findByNumero(numero).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        conta.setEstado(Conta.ACTIVA);
        return ContaResponse.de(contaRepo.save(conta));
    }

    @Transactional
    public void alterarPin(String numero, AlterarPinRequest req) {
        Conta conta = contaRepo.buscarParaAlterar(numero).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        if (req == null || req.pinActual() == null
                || !encoder.matches(req.pinActual(), conta.getPinHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PIN actual incorrecto");
        }
        if (req.pinNovo() == null || !req.pinNovo().matches("\\d{4}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O PIN novo deve ter exactamente 4 dígitos");
        }
        if (req.pinNovo().equals(req.pinActual())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O PIN novo deve ser diferente do PIN actual");
        }
        if (!Conta.ACTIVA.equals(conta.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A conta não está activa");
        }
        conta.setPinHash(encoder.encode(req.pinNovo()));
        conta.setPrimeiroAcesso(false);
    }


}