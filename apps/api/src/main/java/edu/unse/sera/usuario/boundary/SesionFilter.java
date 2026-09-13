package edu.unse.sera.usuario.boundary;

import edu.unse.sera.usuario.control.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Consulta rol y estado en cada request para que las bajas y cambios rijan de inmediato. */
public class SesionFilter extends OncePerRequestFilter {
  public static final String USUARIO_ID = "sera.usuarioId";
  private final UsuarioService usuarios;

  public SesionFilter(UsuarioService usuarios) {
    this.usuarios = usuarios;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var session = request.getSession(false);
    if (session != null && session.getAttribute(USUARIO_ID) instanceof UUID id) {
      var usuario = usuarios.consultarActivo(id);
      if (usuario.isPresent()) {
        var detalle = usuario.get();
        var authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_" + detalle.rol().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } else {
        session.invalidate();
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
