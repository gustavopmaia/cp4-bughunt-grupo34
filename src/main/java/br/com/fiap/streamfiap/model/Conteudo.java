package br.com.fiap.streamfiap.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conteudos")
public abstract class Conteudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String categoria;

    public int duracaoMinutos;

    private int classificacaoEtaria;
    private boolean disponivel;

    protected Conteudo() {
    }

    protected Conteudo(String titulo, String categoria, int duracaoMinutos, int classificacaoEtaria, boolean disponivel) {
        validarTitulo(titulo);
        validarDuracao(duracaoMinutos);
        validarClassificacaoEtaria(classificacaoEtaria);
        this.titulo = titulo;
        this.categoria = categoria;
        this.duracaoMinutos = duracaoMinutos;
        this.classificacaoEtaria = classificacaoEtaria;
        this.disponivel = disponivel;
    }

    private static void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Titulo do conteudo e obrigatorio");
        }
    }

    private static void validarDuracao(int duracaoMinutos) {
        if (duracaoMinutos <= 0) {
            throw new IllegalArgumentException("Duracao do conteudo deve ser maior que zero: " + duracaoMinutos);
        }
    }

    private static void validarClassificacaoEtaria(int classificacaoEtaria) {
        if (classificacaoEtaria < 0) {
            throw new IllegalArgumentException("Classificacao etaria nao pode ser negativa: " + classificacaoEtaria);
        }
    }

    public double calcularPrecoAluguel() {
        return 9.90;
    }

    public double calcularPrecoPromocional() {
        if (this instanceof Promocionavel) {
            Promocionavel promocionavel = (Promocionavel) this;
            return promocionavel.aplicarPromocao(calcularPrecoAluguel());
        }
        return calcularPrecoAluguel();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { validarTitulo(titulo); this.titulo = titulo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public int getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(int duracaoMinutos) { validarDuracao(duracaoMinutos); this.duracaoMinutos = duracaoMinutos; }

    public int getClassificacaoEtaria() { return classificacaoEtaria; }
    public void setClassificacaoEtaria(int classificacaoEtaria) { validarClassificacaoEtaria(classificacaoEtaria); this.classificacaoEtaria = classificacaoEtaria; }

    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
}
