package edu.unse.sera.usuario.boundary;

import edu.unse.sera.usuario.boundary.dto.ActualizarUsuarioRequest;
import edu.unse.sera.usuario.boundary.dto.CambiarPasswordRequest;
import edu.unse.sera.usuario.boundary.dto.CrearUsuarioRequest;
import edu.unse.sera.usuario.boundary.dto.PerfilRequest;
import edu.unse.sera.usuario.boundary.dto.UsuarioResponse;
import edu.unse.sera.usuario.control.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping("/me")
  public UsuarioResponse actual(Principal principal) {
    return UsuarioResponse.from(
        usuarioService.consultarDetalle(UUID.fromString(principal.getName())));
  }

  @PutMapping("/me")
  public UsuarioResponse perfil(Principal principal, @Valid @RequestBody PerfilRequest request) {
    return UsuarioResponse.from(
        usuarioService.actualizarPerfil(
            UUID.fromString(principal.getName()),
            request.nombreCompleto(),
            request.email(),
            request.dni()));
  }

  @PostMapping
  public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
    UsuarioResponse response =
        UsuarioResponse.from(
            usuarioService.registrarUsuario(
                request.nombreCompleto(),
                request.email(),
                request.dni(),
                request.rol(),
                request.password()));
    return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
  }

  @GetMapping
  public List<UsuarioResponse> listar(
      @RequestParam(name = "buscar", required = false) String criterio) {
    return usuarioService.listar(criterio).stream().map(UsuarioResponse::from).toList();
  }

  @GetMapping("/{id}")
  public UsuarioResponse obtener(@PathVariable UUID id) {
    return UsuarioResponse.from(usuarioService.consultarDetalle(id));
  }

  @PutMapping("/{id}")
  public UsuarioResponse actualizar(
      @PathVariable UUID id, @Valid @RequestBody ActualizarUsuarioRequest request) {
    return UsuarioResponse.from(
        usuarioService.actualizarUsuario(
            id,
            request.nombreCompleto(),
            request.email(),
            request.dni(),
            request.rol(),
            request.estadoCuenta()));
  }

  @PutMapping("/{id}/password")
  public ResponseEntity<Void> cambiarPassword(
      @PathVariable UUID id, @Valid @RequestBody CambiarPasswordRequest request) {
    usuarioService.actualizarPasswordUsuario(id, request.password());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
    usuarioService.darDeBaja(id);
    return ResponseEntity.noContent().build();
  }
}
