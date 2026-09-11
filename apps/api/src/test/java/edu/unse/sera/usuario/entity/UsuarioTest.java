package edu.unse.sera.usuario.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UsuarioTest {

  @Test
  void normalizaLosDatosAlCrearUnUsuario() {
    Usuario usuario =
        new Usuario(
            "  Ada Lovelace  ",
            "  ADA@EXAMPLE.COM  ",
            12345678,
            EstadoUsuario.ACTIVO,
            RolUsuario.USUARIO,
            "hash");

    assertThat(usuario.getNombreCompleto()).isEqualTo("Ada Lovelace");
    assertThat(usuario.getEmail()).isEqualTo("ada@example.com");
    assertThat(usuario.getRol()).isEqualTo(RolUsuario.USUARIO);
    assertThat(usuario.getQrUsuario()).matches("SERA-U[a-z0-9]{12}");
  }

  @Test
  void rechazaDatosQueRompenLasReglasBasicas() {
    assertThatThrownBy(
            () ->
                new Usuario(
                    " ",
                    "ada@example.com",
                    12345678,
                    EstadoUsuario.ACTIVO,
                    RolUsuario.USUARIO,
                    "hash"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nombre");

    assertThatThrownBy(
            () ->
                new Usuario(
                    "Ada Lovelace",
                    "ada@example.com",
                    0,
                    EstadoUsuario.ACTIVO,
                    RolUsuario.USUARIO,
                    "hash"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DNI");
  }
}
