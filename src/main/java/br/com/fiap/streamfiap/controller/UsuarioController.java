package br.com.fiap.streamfiap.controller;

import br.com.fiap.streamfiap.model.Usuario;
import br.com.fiap.streamfiap.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // recria a instância para ignorar um id que o cliente tenha enviado no corpo
    @PostMapping
    public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) {
        Usuario novo = new Usuario(usuario.getNome(), usuario.getIdade(), usuario.getCreditos());
        return ResponseEntity.status(201).body(usuarioRepository.save(novo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
        return ResponseEntity.ok(usuario);
    }
}
