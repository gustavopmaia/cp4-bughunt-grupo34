package br.com.fiap.streamfiap.controller;

import br.com.fiap.streamfiap.exception.ConteudoNaoEncontradoException;
import br.com.fiap.streamfiap.model.Conteudo;
import br.com.fiap.streamfiap.model.Documentario;
import br.com.fiap.streamfiap.model.Filme;
import br.com.fiap.streamfiap.model.Serie;
import br.com.fiap.streamfiap.repository.ConteudoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conteudos")
public class ConteudoController {

    @Autowired
    private ConteudoRepository conteudoRepository;

    @GetMapping
    public List<Conteudo> listarTodos() {
        return conteudoRepository.findAll();
    }

    @GetMapping("/{id}")
    public Conteudo buscarPorId(@PathVariable Long id) {
        return buscarConteudoOuFalhar(id);
    }

    @GetMapping("/categoria/{categoria}")
    public List<Conteudo> listarPorCategoria(@PathVariable String categoria) {
        return conteudoRepository.findByCategoria(categoria);
    }

    @GetMapping("/{id}/preco-promocional")
    public double precoPromocional(@PathVariable Long id) {
        return buscarConteudoOuFalhar(id).calcularPrecoPromocional();
    }

    private Conteudo buscarConteudoOuFalhar(Long id) {
        return conteudoRepository.findById(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException("Conteúdo não encontrado: " + id));
    }

    // recria a instância para ignorar um id que o cliente tenha enviado no corpo
    @PostMapping("/filme")
    public ResponseEntity<Filme> cadastrarFilme(@RequestBody Filme filme) {
        Filme novo = new Filme(filme.getTitulo(), filme.getCategoria(), filme.getDuracaoMinutos(),
                filme.getClassificacaoEtaria(), filme.isDisponivel(), filme.isEstreia());
        return ResponseEntity.status(201).body(conteudoRepository.save(novo));
    }

    @PostMapping("/serie")
    public ResponseEntity<Serie> cadastrarSerie(@RequestBody Serie serie) {
        Serie nova = new Serie(serie.getTitulo(), serie.getCategoria(), serie.getDuracaoMinutos(),
                serie.getClassificacaoEtaria(), serie.isDisponivel(), serie.getNumeroTemporadas());
        return ResponseEntity.status(201).body(conteudoRepository.save(nova));
    }

    @PostMapping("/documentario")
    public ResponseEntity<Documentario> cadastrarDocumentario(@RequestBody Documentario documentario) {
        Documentario novo = new Documentario(documentario.getTitulo(), documentario.getCategoria(),
                documentario.getDuracaoMinutos(), documentario.getClassificacaoEtaria(),
                documentario.isDisponivel(), documentario.getTema());
        return ResponseEntity.status(201).body(conteudoRepository.save(novo));
    }
}
