package edu.unse.sera.usuario.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private edu.unse.sera.socio.persistence.SocioRepository socios;

  private UsuarioService usuarioService;

  @BeforeEach
  void setUp() {
    usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, socios);
  }

  @Test
  void registraUnUsuarioActivoConPasswordHasheado() {
    when(passwordEncoder.encode("password-seguro")).thenReturn("hash");
    when(usuarioRepository.saveAndFlush(any(Usuario.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UsuarioDetalle detalle =
        usuarioService.registrarUsuario(
            "  Ada Lovelace  ",
            "  ADA@EXAMPLE.COM  ",
            12345678,
            RolUsuario.USUARIO,
            "password-seguro");

    assertThat(detalle.nombreCompleto()).isEqualTo("Ada Lovelace");
    assertThat(detalle.email()).isEqualTo("ada@example.com");
    assertThat(detalle.estadoCuenta()).isEqualTo(EstadoUsuario.ACTIVO);
    verify(passwordEncoder).encode("password-seguro");
    verify(socios).save(any(edu.unse.sera.socio.entity.Socio.class));
    verify(usuarioRepository).saveAndFlush(any(Usuario.class));
  }

  @Test
  void rechazaUnEmailDuplicadoAntesDeHashearElPassword() {
    when(usuarioRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

    assertThatThrownBy(
            () ->
                usuarioService.registrarUsuario(
                    "Ada Lovelace",
                    "ADA@example.com",
                    12345678,
                    RolUsuario.USUARIO,
                    "password-seguro"))
        .isInstanceOf(UsuarioDuplicadoException.class)
        .hasMessageContaining("email");

    verify(passwordEncoder, never()).encode(any());
    verify(usuarioRepository, never()).saveAndFlush(any());
  }

  @Test
  void validaCredencialesSoloParaUsuariosActivos() {
    Usuario usuario = crearUsuario();
    when(usuarioRepository.findByEmailIgnoreCase("ada@example.com"))
        .thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("password-seguro", "hash")).thenReturn(true);

    assertThat(usuarioService.validarCredenciales("ADA@example.com", "password-seguro")).isTrue();

    usuario.cambiarEstado(EstadoUsuario.DESHABILITADO);
    assertThat(usuarioService.validarCredenciales("ADA@example.com", "password-seguro")).isFalse();
  }

  @Test
  void informaCuandoElUsuarioNoExiste() {
    UUID id = UUID.randomUUID();
    when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> usuarioService.consultarDetalle(id))
        .isInstanceOf(UsuarioNoEncontradoException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void autenticarRechazaPasswordIncorrectoYCuentaInactiva() {
    Usuario usuario = crearUsuario();
    when(usuarioRepository.findByEmailIgnoreCase("ada@example.com"))
        .thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);
    when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);
    assertThat(usuarioService.autenticar(" ADA@example.com ", "incorrecta")).isEmpty();
    assertThat(usuarioService.autenticar(" ADA@example.com ", "correcta")).isPresent();
    usuario.cambiarEstado(EstadoUsuario.INACTIVO);
    assertThat(usuarioService.autenticar("ada@example.com", "correcta")).isEmpty();
  }

  @Test
  void perfilConservaRolEstadoYQr() {
    UUID id = UUID.randomUUID();
    Usuario usuario = crearUsuario();
    String qr = usuario.getQrUsuario();
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
    UsuarioDetalle detalle =
        usuarioService.actualizarPerfil(id, "Otro nombre", "nuevo@example.com", 23456789);
    assertThat(detalle.nombreCompleto()).isEqualTo("Otro nombre");
    assertThat(detalle.email()).isEqualTo("nuevo@example.com");
    assertThat(detalle.rol()).isEqualTo(usuario.getRol());
    assertThat(detalle.estadoCuenta()).isEqualTo(EstadoUsuario.ACTIVO);
    assertThat(detalle.qrUsuario()).isEqualTo(qr);
    verify(usuarioRepository).flush();
  }

  @Test
  void rechazaPasswordQueExcedeLimiteUtf8DeBcrypt() {
    assertThatThrownBy(
            () ->
                usuarioService.registrarUsuario(
                    "Ada", "ada@example.com", 12345678, RolUsuario.USUARIO, "á".repeat(40)))
        .isInstanceOf(PasswordInvalidaException.class);
    assertThat(usuarioService.autenticar("ada@example.com", "á".repeat(40))).isEmpty();
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  void daDeBajaSinEliminarElRegistro() {
    UUID id = UUID.randomUUID();
    Usuario usuario = crearUsuario();
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

    usuarioService.darDeBaja(id);

    assertThat(usuario.getEstadoCuenta()).isEqualTo(EstadoUsuario.INACTIVO);
    verify(usuarioRepository, never()).delete(any());
  }

  @Test
  void buscaUnUsuarioPorDniExacto() {
    Usuario usuario = crearUsuario();
    when(usuarioRepository.findByDni(12345678)).thenReturn(Optional.of(usuario));

    assertThat(usuarioService.listar("12345678"))
        .extracting(UsuarioDetalle::email)
        .containsExactly("ada@example.com");
  }

  @Test
  void actualizaYFuerzaLaAuditoriaAntesDeResponder() {
    UUID id = UUID.randomUUID();
    Usuario usuario = crearUsuario();
    String qrOriginal = usuario.getQrUsuario();
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

    UsuarioDetalle detalle =
        usuarioService.actualizarUsuario(
            id,
            "Ada Byron",
            "ada.byron@example.com",
            12345678,
            RolUsuario.ADMIN,
            EstadoUsuario.ACTIVO);

    assertThat(detalle.nombreCompleto()).isEqualTo("Ada Byron");
    assertThat(detalle.rol()).isEqualTo(RolUsuario.ADMIN);
    assertThat(detalle.qrUsuario()).isEqualTo(qrOriginal);
    verify(usuarioRepository).flush();
  }

  @Test
  void traduceUnaColisionConcurrenteDeEmailAlRegistrar() {
    when(passwordEncoder.encode("password-seguro")).thenReturn("hash");
    when(usuarioRepository.saveAndFlush(any(Usuario.class)))
        .thenThrow(violacionDeUnicidad("uk_usuarios_email"));

    assertThatThrownBy(
            () ->
                usuarioService.registrarUsuario(
                    "Ada Lovelace",
                    "ada@example.com",
                    12345678,
                    RolUsuario.USUARIO,
                    "password-seguro"))
        .isInstanceOf(UsuarioDuplicadoException.class)
        .hasMessageContaining("email")
        .hasCauseInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void traduceUnaColisionConcurrenteDeDniAlActualizar() {
    UUID id = UUID.randomUUID();
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(crearUsuario()));
    DataIntegrityViolationException violation = violacionDeUnicidad("uk_usuarios_dni");
    org.mockito.Mockito.doThrow(violation).when(usuarioRepository).flush();

    assertThatThrownBy(
            () ->
                usuarioService.actualizarUsuario(
                    id,
                    "Ada Byron",
                    "ada.byron@example.com",
                    87654321,
                    RolUsuario.ADMIN,
                    EstadoUsuario.ACTIVO))
        .isInstanceOf(UsuarioDuplicadoException.class)
        .hasMessageContaining("dni")
        .hasCause(violation);
  }

  @Test
  void noDisfrazaOtraViolacionDeIntegridadComoDuplicado() {
    when(passwordEncoder.encode("password-seguro")).thenReturn("hash");
    DataIntegrityViolationException violation =
        new DataIntegrityViolationException("Otra restricción");
    when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(violation);

    assertThatThrownBy(
            () ->
                usuarioService.registrarUsuario(
                    "Ada Lovelace",
                    "ada@example.com",
                    12345678,
                    RolUsuario.USUARIO,
                    "password-seguro"))
        .isSameAs(violation);
  }

  private Usuario crearUsuario() {
    return new Usuario(
        "Ada Lovelace",
        "ada@example.com",
        12345678,
        EstadoUsuario.ACTIVO,
        RolUsuario.USUARIO,
        "hash");
  }

  private DataIntegrityViolationException violacionDeUnicidad(String constraintName) {
    SQLException sqlException = new SQLException("Clave duplicada", "23505");
    ConstraintViolationException constraintViolation =
        new ConstraintViolationException("Restricción única", sqlException, constraintName);
    return new DataIntegrityViolationException("No se pudo guardar", constraintViolation);
  }
}
