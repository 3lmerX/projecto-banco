package mz.banco.auditoria;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String tipoAtor;

    @Column(nullable = false, length = 255)
    private String ator;

    @Column(nullable = false, length = 100)
    private String accao;

    @Column(length = 500)
    private String detalhe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getTipoAtor() { return tipoAtor; }
    public void setTipoAtor(String tipoAtor) { this.tipoAtor = tipoAtor; }
    public String getAtor() { return ator; }
    public void setAtor(String ator) { this.ator = ator; }
    public String getAccao() { return accao; }
    public void setAccao(String accao) { this.accao = accao; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
}
