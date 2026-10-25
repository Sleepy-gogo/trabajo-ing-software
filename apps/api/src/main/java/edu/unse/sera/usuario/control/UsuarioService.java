package edu.unse.sera.usuario.control;

import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.socio.persistence.SocioRepository;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.nio.charset.StandardCharsets;
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
  private final SocioRepository socios;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      SocioRepository socios) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.socios = socios;
  }

  public UsuarioDetalle registrarUsuario(
      String nombreCompleto, String email, int dni, RolUsuario rol, String rawPassword) {
    return registrarUsuario(
        nombreCompleto, email, dni, rol, rawPassword, RelacionUnse.EXTERNO, null);
  }

  public UsuarioDetalle registrarUsuario(
      String nombreCompleto,
      String email,
      int dni,
      RolUsuario rol,
      String rawPassword,
      RelacionUnse relacion,
      String identificador) {
    validarPassword(rawPassword);
    validarUnicidad(email, dni, null);
    String hashedPassword = passwordEncoder.encode(rawPassword);
    Usuario usuario =
        new Usuario(nombreCompleto, email, dni, EstadoUsuario.ACTIVO, rol, hashedPassword);
    try {
      usuarioRepository.saveAndFlush(usuario);
      socios.save(
          new Socio(
              usuario,
              relacion == null ? RelacionUnse.EXTERNO : relacion,
              EstadoVerificacionUnse.PENDIENTE,
              identificador));
      return toResponse(usuario);
    } catch (DataIntegrityViolationException exception) {
      throw traducirConflictoDeUnicidad(exception);
    }
  }

  @Transactional(readOnly = true)
  public Optional<UsuarioDetalle> autenticar(String email, String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      return Optional.empty();
    }
    return usuarioRepository
        .findByEmailIgnoreCase(normalizarEmail(email))
        .filter(usuario -> passwordEncoder.matches(password, usuario.getPasswordHash()))
        .filter(usuario -> usuario.getEstadoCuenta() == EstadoUsuario.ACTIVO)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public Optional<UsuarioDetalle> consultarActivo(UUID id) {
    return usuarioRepository
        .findById(id)
        .filter(usuario -> usuario.getEstadoCuenta() == EstadoUsuario.ACTIVO)
        .map(this::toResponse);
  }

  public UsuarioDetalle actualizarPerfil(UUID id, String nombreCompleto, String email, int dni) {
    Usuario usuario = buscar(id);
    return actualizarUsuario(
        id, nombreCompleto, email, dni, usuario.getRol(), usuario.getEstadoCuenta());
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

  public void darDeBaja(UUID id) {
    buscar(id).cambiarEstado(EstadoUsuario.INACTIVO);
  }

  public void actualizarPasswordUsuario(UUID id, String rawPassword) {
    validarPassword(rawPassword);
    Usuario usuario = buscar(id);
    usuario.cambiarPasswordHash(passwordEncoder.encode(rawPassword));
  }

  public Usuario buscar(UUID id) {
    return usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
  }

  private void validarPassword(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new PasswordInvalidaException();
    }
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

  private RuntimeException traducirConflictoDeUnicidad(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException violation) {
        String constraintName = violation.getConstraintName();
        if (EMAIL_UNIQUE_CONSTRAINT.equals(constraintName)) {
          return new UsuarioDuplicadoException("email", exception);
        }
        if (DNI_UNIQUE_CONSTRAINT.equals(constraintName)) {
          return new UsuarioDuplicadoException("dni", exception);
        }
        return exception;
      }
      cause = cause.getCause();
    }
    return exception;
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
