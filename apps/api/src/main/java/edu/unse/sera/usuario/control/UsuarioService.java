package edu.unse.sera.usuario.control;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import java.util.UUID;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.entity.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;

  private PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public UsuarioDetalle registrarUsuario(String nombreCompleto, String email, int dni, String rawPassword) {
    String hashedPassword = passwordEncoder.encode(rawPassword);
    Usuario user = new Usuario(nombreCompleto, email, dni, EstadoUsuario.ACTIVO, hashedPassword);
    return toResponse(usuarioRepository.save(user));
  }

  public boolean validarCredenciales(String email, String rawPassword) {
    Usuario user = usuarioRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return false;
    }
    return passwordEncoder.matches(rawPassword, user.getPasswordHash());
  }

  public Usuario buscar(UUID id) {
    return usuarioRepository.findById(id).orElseThrow(()-> new UsuarioNoEncontradoException(id));
  }

  // SOBRECARGAR DE SER NECESARIO.
  @Transactional(readOnly = true)
  public UsuarioDetalle buscarUsuario(UUID id) {
    return toResponse(buscar(id));
  }



  public UsuarioDetalle actualizarUsuario(UUID id, String nombreCompleto, String email, int dni, EstadoUsuario estadoCuenta) {
    Usuario user = buscar(id);
    user.actualizar(nombreCompleto,email,dni,estadoCuenta);
    return toResponse(user);
  }

  /*actualizarUsuario(datos)
buscarUsuario(criterio)
consultarDetalle(idUsuario)
crearUsuarioAdministrativo(datos,rol)
obtenerDatosUsuario(idUsuario)

*/

  private UsuarioDetalle toResponse(Usuario usuario) {
    return new UsuarioDetalle(usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getDni());
  }

}
