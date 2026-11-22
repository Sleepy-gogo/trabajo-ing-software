package edu.unse.sera.actividad.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistroActividadTest {
  private final OffsetDateTime ahora = OffsetDateTime.parse("2026-11-17T13:00:00Z");

  @Test
  void recortaLosTextosAlTamanoDeLasColumnas() {
    var registro =
        new RegistroActividad(
            null,
            " " + "a".repeat(120) + " ",
            TipoActividad.INICIO_SESION_FALLIDO,
            "d".repeat(600),
            ahora);
    assertThat(registro.getEmail()).hasSize(100);
    assertThat(registro.getDetalle()).hasSize(500);
    assertThat(registro.getUsuarioId()).isNull();
  }

  @Test
  void guardaVacioComoAusente() {
    UUID usuario = UUID.randomUUID();
    var registro = new RegistroActividad(usuario, "  ", TipoActividad.CIERRE_SESION, null, ahora);
    assertThat(registro.getEmail()).isNull();
    assertThat(registro.getDetalle()).isNull();
    assertThat(registro.getUsuarioId()).isEqualTo(usuario);
    assertThat(registro.getCreadoEn()).isEqualTo(ahora);
  }

  @Test
  void exigeTipoYFecha() {
    assertThatThrownBy(() -> new RegistroActividad(null, "a@b.c", null, null, ahora))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> new RegistroActividad(null, "a@b.c", TipoActividad.INICIO_SESION, null, null))
        .isInstanceOf(NullPointerException.class);
  }
}
