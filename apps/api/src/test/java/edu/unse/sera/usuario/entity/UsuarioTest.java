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
            "  socio  ",
            "  QR-ADA-001  ",
            "hash");

    assertThat(usuario.getId()).isNotNull();
    assertThat(usuario.getNombreCompleto()).isEqualTo("Ada Lovelace");
    assertThat(usuario.getEmail()).isEqualTo("ada@example.com");
    assertThat(usuario.getRol()).isEqualTo("socio");
    assertThat(usuario.getQrUsuario()).isEqualTo("QR-ADA-001");
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
                    "socio",
                    "QR-ADA-001",
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
                    "socio",
                    "QR-ADA-001",
                    "hash"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DNI");
  }
}
