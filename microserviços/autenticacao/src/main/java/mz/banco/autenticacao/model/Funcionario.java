package mz.banco.autenticacao.model;

import jakarta.persistence.*;

@Entity
@Table(name = "funcionarios")
public class Funcionario {

    public static final String ACTIVO = "ACTIVO";
    public static final String BLOQUEADO = "BLOQUEADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String palavraPasse; // Guardada de forma cifrada (BCrypt)

    private String estado = ACTIVO;

    // --- Construtores ---
    public Funcionario() {}

    public Funcionario(String nome, String email, String palavraPasse, String estado) {
        this.nome = nome;
        this.email = email;
        this.palavraPasse = palavraPasse;
        this.estado = estado;
    }

    // --- Getters e Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPalavraPasse() {
        return palavraPasse;
    }

    public void setPalavraPasse(String palavraPasse) {
        this.palavraPasse = palavraPasse;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}