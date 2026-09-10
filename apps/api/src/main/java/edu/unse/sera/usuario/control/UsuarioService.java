package edu.unse.sera.usuario.control;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;

  private PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public UsuarioDetalle registrarUsuario(String nombreCompleto, String email, int dni,
    RolUsuario rol, String rawPassword) {
    String hashedPassword = passwordEncoder.encode(rawPassword);
    // TODO: VALIDACION

    Usuario user = new Usuario(nombreCompleto, email, dni, EstadoUsuario.ACTIVO, rol,
      hashedPassword);
    return toResponse(usuarioRepository.save(user));
  }

  public boolean validarCredenciales(String email, String rawPassword) {
    Usuario user = buscarPorEmail(email);
    return passwordEncoder.matches(rawPassword, user.getPasswordHash());
  }


  public Usuario buscar(UUID id) {
    return usuarioRepository.findById(id)
      .orElseThrow(() -> new UsuarioNoEncontradoException("id " + id.toString()));
  }


  public Usuario buscarPorEmail(String email) {
    return usuarioRepository.findByEmail(email)
      .orElseThrow(() -> new UsuarioNoEncontradoException("email " + email));
  }

  public Usuario buscarPorDni(int dni) {
    return usuarioRepository.findByDni(dni)
      .orElseThrow(() -> new UsuarioNoEncontradoException("dni " + dni));
  }

  // TODO: LA CONSULTA POR CRITERIOS
  // TODO: LISTAR lo que sea

  public UsuarioDetalle actualizarPasswordUsuario(UUID id, String rawPassword) {
    Usuario user = buscar(id);

    String hashedPassword = passwordEncoder.encode(rawPassword);
    user.setPasswordHash(hashedPassword);
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  public UsuarioDetalle consultarDetalle(UUID id) {
    return toResponse(buscar(id));
  }

  public UsuarioDetalle actualizarUsuario(UUID id, String nombreCompleto, String email, int dni,
    EstadoUsuario estadoCuenta) {
    Usuario user = buscar(id);
    user.actualizar(nombreCompleto, email, dni, estadoCuenta);
    return toResponse(user);
  }


  private UsuarioDetalle toResponse(Usuario usuario) {
    return new UsuarioDetalle(usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(),
      usuario.getDni(), usuario.getRolUsuario(), usuario.getQrCode(), usuario.getEstadoCuenta());
  }

}
