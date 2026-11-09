package mz.banco.contas;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class ConcorrenciaTest {
    @Autowired private ContaService service;
    @Autowired private ContaRepository contaRepository;
    @Autowired private ClienteRepository clienteRepository;
    private ExecutorService executor;

    @AfterEach
    void limparExecutor() {
        if (executor != null) executor.shutdownNow();
    }

    @Test
    void doisDepositosSimultaneos() throws Exception {
        Conta conta = criarConta("1000000001", "0.00");
        String numeroPrimeira = conta.getNumero();
        executor = Executors.newFixedThreadPool(20);
        executarSimultaneamente(List.of(
                () -> service.depositar(numeroPrimeira, new DepositoRequest(new BigDecimal("100.00"))),
                () -> service.depositar(numeroPrimeira, new DepositoRequest(new BigDecimal("50.00")))));
        assertEquals(new BigDecimal("150.00"), saldo(numeroPrimeira));

        conta = criarConta("1000000002", "0.00");
        String numero = conta.getNumero();
        List<java.util.concurrent.Callable<Void>> deposits = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            deposits.add(() -> {
                service.depositar(numero, new DepositoRequest(new BigDecimal("10.00")));
                return null;
            });
        }
        executarSimultaneamente(deposits);
        assertEquals(new BigDecimal("200.00"), saldo(numero));
    }

    @Test
    void duasTransferenciasSimultaneasSoUmaPassa() throws Exception {
        Conta origem = criarConta("2000000001", "100.00");
        Conta destino = criarConta("2000000002", "0.00");
        executor = Executors.newFixedThreadPool(2);
        AtomicInteger sucesso = new AtomicInteger();
        AtomicInteger conflito = new AtomicInteger();
        List<java.util.concurrent.Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> {
                try {
                    service.transferir(new TransferenciaInternaRequest(origem.getNumero(),
                            destino.getNumero(), new BigDecimal("100.00"), "1234"));
                    sucesso.incrementAndGet();
                } catch (ResponseStatusException e) {
                    assertEquals(409, e.getStatusCode().value());
                    conflito.incrementAndGet();
                }
                return null;
            });
        }
        executarSimultaneamente(tasks);
        assertEquals(1, sucesso.get());
        assertEquals(1, conflito.get());
        assertEquals(new BigDecimal("0.00"), saldo(origem.getNumero()));
        assertEquals(new BigDecimal("100.00"), saldo(destino.getNumero()));
        assertTrue(saldo(origem.getNumero()).signum() >= 0);
    }

    @Test
    void transferenciasCruzadasSemDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            Conta a = criarConta("3000000001", "1000.00");
            Conta b = criarConta("3000000002", "1000.00");
            executor = Executors.newFixedThreadPool(2);
            for (int i = 0; i < 20; i++) {
                String origem = i % 2 == 0 ? a.getNumero() : b.getNumero();
                String destino = i % 2 == 0 ? b.getNumero() : a.getNumero();
                executarSimultaneamente(List.of(
                        () -> transferir(origem, destino),
                        () -> transferir(destino, origem)));
            }
            assertEquals(new BigDecimal("2000.00"),
                    saldo(a.getNumero()).add(saldo(b.getNumero())));
        });
    }

    private Void transferir(String origem, String destino) {
        service.transferir(new TransferenciaInternaRequest(origem, destino,
                new BigDecimal("10.00"), "1234"));
        return null;
    }

    private Conta criarConta(String numero, String valor) {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente de teste " + numero);
        cliente.setNumeroBi("BI-" + numero);
        cliente.setTelefone("84" + numero);
        cliente.setEstado(Cliente.ACTIVO);
        cliente = clienteRepository.saveAndFlush(cliente);

        Conta conta = new Conta();
        conta.setNumero(numero);
        conta.setCliente(cliente);
        conta.setPinHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("1234"));
        conta.setSaldo(new BigDecimal(valor));
        conta.setEstado(Conta.ACTIVA);
        conta.setPrimeiroAcesso(false);
        return contaRepository.saveAndFlush(conta);
    }

    private BigDecimal saldo(String numero) {
        return contaRepository.findByNumero(numero).orElseThrow().getSaldo();
    }

    private <T> List<T> executarSimultaneamente(List<java.util.concurrent.Callable<T>> tasks)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Threads não iniciaram em simultâneo");
                }
                return task.call();
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
        return results;
    }
}
