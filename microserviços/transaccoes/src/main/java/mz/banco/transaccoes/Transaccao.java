package mz.banco.transaccoes;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transaccao {

    public static final String PENDENTE = "PENDENTE";
    public static final String CONCLUIDA = "CONCLUIDA";
    public static final String FALHADA = "FALHADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referencia;

    private String contaOrigem;      // vazio nos depósitos
    private String contaDestino;

    @Column(precision = 19, scale = 2)
    private BigDecimal valor;

    private LocalDateTime dataHora = LocalDateTime.now();
    private String tipo;             // DEPOSITO ou TRANSFERENCIA
    private String estado;           // PENDENTE, CONCLUIDA ou FALHADA
    private String motivo;           // porque falhou (se falhou)
    private String registadoPor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getContaOrigem() {
        return contaOrigem;
    }

    public void setContaOrigem(String contaOrigem) {
        this.contaOrigem = contaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(String contaDestino) {
        this.contaDestino = contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getRegistadoPor() { return registadoPor; }

    public void setRegistadoPor(String registadoPor) { this.registadoPor = registadoPor; }
}