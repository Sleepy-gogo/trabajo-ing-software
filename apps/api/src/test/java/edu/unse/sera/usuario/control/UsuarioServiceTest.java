package edu.unse.sera.usuario.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UsuarioService usuarioService;

  @BeforeEach
  void setUp() {
    usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
  }

  @Test
  void registraUnUsuarioActivoConPasswordHasheado() {
    when(passwordEncoder.encode("password-seguro")).thenReturn("hash");
    when(usuarioRepository.save(any(Usuario.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UsuarioDetalle detalle =
        usuarioService.registrarUsuario(
            "  Ada Lovelace  ",
            "  ADA@EXAMPLE.COM  ",
            12345678,
            "socio",
            "QR-ADA-001",
            "password-seguro");

    assertThat(detalle.nombreCompleto()).isEqualTo("Ada Lovelace");
    assertThat(detalle.email()).isEqualTo("ada@example.com");
    assertThat(detalle.estadoCuenta()).isEqualTo(EstadoUsuario.ACTIVO);
    verify(passwordEncoder).encode("password-seguro");
    verify(usuarioRepository).save(any(Usuario.class));
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
                    "socio",
                    "QR-ADA-001",
                    "password-seguro"))
        .isInstanceOf(UsuarioDuplicadoException.class)
        .hasMessageContaining("email");

    verify(passwordEncoder, never()).encode(any());
    verify(usuarioRepository, never()).save(any());
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

  private Usuario crearUsuario() {
    return new Usuario(
        "Ada Lovelace",
        "ada@example.com",
        12345678,
        EstadoUsuario.ACTIVO,
        "socio",
        "QR-ADA-001",
        "hash");
  }
}
