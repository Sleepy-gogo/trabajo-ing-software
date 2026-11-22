package edu.unse.sera.usuario.control;

import edu.unse.sera.actividad.control.ActividadService;
import edu.unse.sera.actividad.entity.TipoActividad;
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
  private final ActividadService actividad;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      SocioRepository socios,
      ActividadService actividad) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.socios = socios;
    this.actividad = actividad;
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
      RelacionUnse declarada = relacion == null ? RelacionUnse.EXTERNO : relacion;
      socios.save(
          new Socio(usuario, declarada, Socio.verificacionInicial(declarada), identificador));
      return toResponse(usuario);
    } catch (DataIntegrityViolationException exception) {
      throw traducirConflictoDeUnicidad(exception);
    }
  }

  /**
   * Comprueba las credenciales y registra el intento en el historial de actividad. Un usuario común
   * necesita además una relación con la UNSE verificada. El motivo del bloqueo solo se informa
   * cuando la contraseña es correcta, para no revelar el estado de una cuenta ajena.
   */
  @Transactional(readOnly = true)
  public Optional<UsuarioDetalle> autenticar(String email, String password) {
    String emailNormalizado = normalizarEmail(email);
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      return rechazar(null, emailNormalizado, "Contraseña con formato inválido");
    }
    Optional<Usuario> cuenta = usuarioRepository.findByEmailIgnoreCase(emailNormalizado);
    if (cuenta.isEmpty()) {
      return rechazar(null, emailNormalizado, "El correo no corresponde a ninguna cuenta");
    }
    Usuario usuario = cuenta.get();
    if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
      return rechazar(usuario.getId(), usuario.getEmail(), "Contraseña incorrecta");
    }
    if (usuario.getEstadoCuenta() != EstadoUsuario.ACTIVO) {
      return rechazar(usuario.getId(), usuario.getEmail(), "La cuenta no está activa");
    }
    Optional<EstadoVerificacionUnse> verificacion = relacionSinVerificar(usuario);
    if (verificacion.isPresent()) {
      actividad.registrar(
          usuario.getId(),
          usuario.getEmail(),
          TipoActividad.INICIO_SESION_BLOQUEADO,
          "Relación con la UNSE " + verificacion.get().name().toLowerCase(Locale.ROOT));
      throw new RelacionNoVerificadaException(verificacion.get());
    }
    actividad.registrar(
        usuario.getId(),
        usuario.getEmail(),
        TipoActividad.INICIO_SESION,
        "Rol " + usuario.getRol().name().toLowerCase(Locale.ROOT));
    return Optional.of(toResponse(usuario));
  }

  private Optional<UsuarioDetalle> rechazar(UUID usuarioId, String email, String motivo) {
    actividad.registrar(usuarioId, email, TipoActividad.INICIO_SESION_FALLIDO, motivo);
    return Optional.empty();
  }

  public void registrarCierreSesion(UUID id) {
    usuarioRepository
        .findById(id)
        .ifPresent(
            u -> actividad.registrar(u.getId(), u.getEmail(), TipoActividad.CIERRE_SESION, null));
  }

  @Transactional(readOnly = true)
  public Optional<UsuarioDetalle> consultarActivo(UUID id) {
    return usuarioRepository
        .findById(id)
        .filter(usuario -> usuario.getEstadoCuenta() == EstadoUsuario.ACTIVO)
        .filter(usuario -> relacionSinVerificar(usuario).isEmpty())
        .map(this::toResponse);
  }

  /** ADMIN y STAFF no dependen de una relación declarada: sus roles los asigna administración. */
  private Optional<EstadoVerificacionUnse> relacionSinVerificar(Usuario usuario) {
    if (usuario.getRol() != RolUsuario.USUARIO) {
      return Optional.empty();
    }
    return socios
        .findByUsuarioId(usuario.getId())
        .filter(socio -> !socio.habilitaIngreso())
        .map(Socio::getEstadoVerificacionUnse);
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
