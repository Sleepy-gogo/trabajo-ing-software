package edu.unse.sera.usuario.control;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public UsuarioDetalle registrarUsuario(
      String nombreCompleto,
      String email,
      int dni,
      String rol,
      String qrUsuario,
      String rawPassword) {
    validarUnicidad(email, dni, null);
    String hashedPassword = passwordEncoder.encode(rawPassword);
    Usuario usuario =
        new Usuario(
            nombreCompleto, email, dni, EstadoUsuario.ACTIVO, rol, qrUsuario, hashedPassword);
    return toResponse(usuarioRepository.saveAndFlush(usuario));
  }

  @Transactional(readOnly = true)
  public boolean validarCredenciales(String email, String rawPassword) {
    return usuarioRepository
        .findByEmailIgnoreCase(normalizarEmail(email))
        .filter(usuario -> usuario.getEstadoCuenta() == EstadoUsuario.ACTIVO)
        .map(usuario -> passwordEncoder.matches(rawPassword, usuario.getPasswordHash()))
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public List<UsuarioDetalle> listar(String criterio) {
    List<Usuario> usuarios;
    if (criterio == null || criterio.isBlank()) {
      usuarios = usuarioRepository.findAllByOrderByNombreCompletoAsc();
    } else {
      String valor = criterio.trim();
      usuarios =
          convertirDni(valor)
              .flatMap(usuarioRepository::findByDni)
              .map(List::of)
              .orElseGet(
                  () ->
                      usuarioRepository
                          .findByNombreCompletoContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNombreCompletoAsc(
                              valor, valor));
    }
    return usuarios.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public UsuarioDetalle consultarDetalle(UUID id) {
    return toResponse(buscar(id));
  }

  public UsuarioDetalle actualizarUsuario(
      UUID id,
      String nombreCompleto,
      String email,
      int dni,
      String rol,
      String qrUsuario,
      EstadoUsuario estadoCuenta) {
    Usuario usuario = buscar(id);
    validarUnicidad(email, dni, id);
    usuario.actualizarDatos(nombreCompleto, email, dni, rol, qrUsuario);
    usuario.cambiarEstado(estadoCuenta);
    usuarioRepository.flush();
    return toResponse(usuario);
  }

  public void darDeBaja(UUID id) {
    buscar(id).cambiarEstado(EstadoUsuario.INACTIVO);
  }

  public void actualizarPasswordUsuario(UUID id, String rawPassword) {
    Usuario usuario = buscar(id);
    usuario.cambiarPasswordHash(passwordEncoder.encode(rawPassword));
  }

  private Usuario buscar(UUID id) {
    return usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
  }

  private void validarUnicidad(String email, int dni, UUID usuarioId) {
    String emailNormalizado = normalizarEmail(email);
    boolean emailEnUso =
        usuarioId == null
            ? usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)
            : usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, usuarioId);
    if (emailEnUso) {
      throw new UsuarioDuplicadoException("email");
    }

    boolean dniEnUso =
        usuarioId == null
            ? usuarioRepository.existsByDni(dni)
            : usuarioRepository.existsByDniAndIdNot(dni, usuarioId);
    if (dniEnUso) {
      throw new UsuarioDuplicadoException("dni");
    }
  }

  private String normalizarEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private Optional<Integer> convertirDni(String valor) {
    try {
      return Optional.of(Integer.valueOf(valor));
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private UsuarioDetalle toResponse(Usuario usuario) {
    return new UsuarioDetalle(
        usuario.getId(),
        usuario.getNombreCompleto(),
        usuario.getEmail(),
        usuario.getDni(),
        usuario.getRol(),
        usuario.getQrUsuario(),
        usuario.getEstadoCuenta(),
        usuario.getCreatedAt(),
        usuario.getUpdatedAt());
  }
}
