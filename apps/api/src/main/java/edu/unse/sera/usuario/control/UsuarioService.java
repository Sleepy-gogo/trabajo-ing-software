package edu.unse.sera.usuario.control;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {

  private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_usuarios_email";
  private static final String DNI_UNIQUE_CONSTRAINT = "uk_usuarios_dni";

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /** Registra una cuenta activa y guarda solo el hash de su contraseña. */
  public UsuarioDetalle registrarUsuario(
      String nombreCompleto, String email, int dni, RolUsuario rol, String rawPassword) {
    validarUnicidad(email, dni, null);
    String hashedPassword = passwordEncoder.encode(rawPassword);
    Usuario usuario =
        new Usuario(nombreCompleto, email, dni, EstadoUsuario.ACTIVO, rol, hashedPassword);
    try {
      return toResponse(usuarioRepository.saveAndFlush(usuario));
    } catch (DataIntegrityViolationException exception) {
      throw traducirConflictoDeUnicidad(exception);
    }
  }

  /** No distingue cuentas inexistentes, inactivas o contraseñas erróneas ante quien llama. */
  @Transactional(readOnly = true)
  public boolean validarCredenciales(String email, String rawPassword) {
    return usuarioRepository
        .findByEmailIgnoreCase(normalizarEmail(email))
        .filter(usuario -> usuario.getEstadoCuenta() == EstadoUsuario.ACTIVO)
        .map(usuario -> passwordEncoder.matches(rawPassword, usuario.getPasswordHash()))
        .orElse(false);
  }

  /** Reúne el listado administrativo y su búsqueda en un único caso de uso. */
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

  /** Devuelve el detalle administrativo sin exponer la entidad persistente. */
  @Transactional(readOnly = true)
  public UsuarioDetalle consultarDetalle(UUID id) {
    return toResponse(buscar(id));
  }

  /** Edita los datos administrativos sin reemplazar el QR estable del usuario. */
  public UsuarioDetalle actualizarUsuario(
      UUID id,
      String nombreCompleto,
      String email,
      int dni,
      RolUsuario rol,
      EstadoUsuario estadoCuenta) {
    Usuario usuario = buscar(id);
    validarUnicidad(email, dni, id);
    usuario.actualizarDatos(nombreCompleto, email, dni, rol);
    usuario.cambiarEstado(estadoCuenta);
    try {
      usuarioRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw traducirConflictoDeUnicidad(exception);
    }
    return toResponse(usuario);
  }

  /** Conserva el registro y sus futuras relaciones, pero impide usar la cuenta. */
  public void darDeBaja(UUID id) {
    buscar(id).cambiarEstado(EstadoUsuario.INACTIVO);
  }

  /** Separa el cambio de contraseña de la edición de datos administrativos. */
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

  /** Mantiene el contrato 409 si la base resuelve una carrera entre dos requests. */
  private RuntimeException traducirConflictoDeUnicidad(DataIntegrityViolationException exception) {
    Optional<ConstraintViolationException> violation = buscarConstraintViolation(exception);
    if (violation.isEmpty()) {
      return exception;
    }

    String constraintName = violation.get().getConstraintName();
    if (EMAIL_UNIQUE_CONSTRAINT.equals(constraintName)) {
      return new UsuarioDuplicadoException("email", exception);
    }
    if (DNI_UNIQUE_CONSTRAINT.equals(constraintName)) {
      return new UsuarioDuplicadoException("dni", exception);
    }
    return exception;
  }

  private Optional<ConstraintViolationException> buscarConstraintViolation(Throwable exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException violation) {
        return Optional.of(violation);
      }
      cause = cause.getCause();
    }
    return Optional.empty();
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
