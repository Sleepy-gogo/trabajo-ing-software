package edu.unse.sera.usuario.boundary;

import edu.unse.sera.shared.boundary.dto.ApiError;
import edu.unse.sera.usuario.boundary.dto.LoginRequest;
import edu.unse.sera.usuario.boundary.dto.RegistroRequest;
import edu.unse.sera.usuario.boundary.dto.UsuarioResponse;
import edu.unse.sera.usuario.control.UsuarioService;
import edu.unse.sera.usuario.entity.RolUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UsuarioService usuarios;

  public AuthController(UsuarioService usuarios) {
    this.usuarios = usuarios;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/registro")
  public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
    var usuario =
        usuarios.registrarUsuario(
            request.nombreCompleto(),
            request.email(),
            request.dni(),
            RolUsuario.USUARIO,
            request.password());
    return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id()))
        .body(UsuarioResponse.from(usuario));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    var usuario = usuarios.autenticar(request.email(), request.password());
    if (usuario.isEmpty()) {
      return ResponseEntity.status(401)
          .body(
              new ApiError(
                  "credenciales_invalidas",
                  "El email o la contraseña no coinciden, o la cuenta no está activa.",
                  Map.of()));
    }
    var anterior = http.getSession(false);
    if (anterior != null) {
      anterior.invalidate();
    }
    http.getSession(true).setAttribute(SesionFilter.USUARIO_ID, usuario.get().id());
    return ResponseEntity.ok(UsuarioResponse.from(usuario.get()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return ResponseEntity.noContent().build();
  }
}
