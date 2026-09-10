package br.com.fiap.streamfiap.model;

import jakarta.persistence.Entity;

@Entity
public class Documentario extends Conteudo {

    private String tema;

    public Documentario() {
    }

    public Documentario(String titulo, String categoria, int duracaoMinutos, int classificacaoEtaria, boolean disponivel, String tema) {
        super(titulo, categoria, duracaoMinutos, classificacaoEtaria, disponivel);
        this.tema = tema;
    }

    /** Documentario e gratuito por contrato e nao participa de promocao. */
    @Override
    public double calcularPrecoAluguel() {
        return 0.0;
    }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
}
