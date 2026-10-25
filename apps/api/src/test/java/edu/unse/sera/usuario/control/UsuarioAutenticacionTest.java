package edu.unse.sera.usuario.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.unse.sera.shared.config.PasswordConfig;
import edu.unse.sera.socio.persistence.SocioRepository;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioAutenticacionTest {
  @Test
  void autenticaConElHashRealYRechazaLaPasswordAnteriorTrasCambiarla() {
    var repositorio = mock(UsuarioRepository.class);
    var encoder = new PasswordConfig().passwordEncoder();
    var usuario =
        new Usuario(
            "Ada",
            "ada@example.com",
            12345678,
            EstadoUsuario.ACTIVO,
            RolUsuario.USUARIO,
            encoder.encode("password-seguro"));
    var servicio = new UsuarioService(repositorio, encoder, mock(SocioRepository.class));
    when(repositorio.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(usuario));
    UUID id = UUID.randomUUID();
    when(repositorio.findById(id)).thenReturn(Optional.of(usuario));

    assertThat(usuario.getPasswordHash()).isNotEqualTo("password-seguro");
    assertThat(servicio.autenticar(" ADA@example.com ", "password-seguro")).isPresent();
    assertThat(servicio.autenticar("ada@example.com", "incorrecta")).isEmpty();

    servicio.actualizarPasswordUsuario(id, "password-nueva");
    assertThat(servicio.autenticar("ada@example.com", "password-seguro")).isEmpty();
    assertThat(servicio.autenticar("ada@example.com", "password-nueva")).isPresent();

    usuario.cambiarEstado(EstadoUsuario.DESHABILITADO);
    assertThat(servicio.autenticar("ada@example.com", "password-nueva")).isEmpty();
  }
}
