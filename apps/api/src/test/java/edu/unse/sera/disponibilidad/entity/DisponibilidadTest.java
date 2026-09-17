package edu.unse.sera.disponibilidad.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.unse.sera.espacio.entity.Espacio;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DisponibilidadTest {

  @Test
  void conservaElEspacioYElRangoHorario() {
    Espacio espacio = espacio();

    Disponibilidad disponibilidad =
        new Disponibilidad(espacio, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0));

    assertThat(disponibilidad.getEspacio()).isSameAs(espacio);
    assertThat(disponibilidad.getDiaSemana()).isEqualTo(DiaSemana.LUNES);
    assertThat(disponibilidad.getHoraDesde()).isEqualTo(LocalTime.of(8, 0));
    assertThat(disponibilidad.getHoraHasta()).isEqualTo(LocalTime.of(12, 0));
  }

  @Test
  void rechazaUnRangoSinDuracion() {
    assertThatThrownBy(
            () ->
                new Disponibilidad(
                    espacio(), DiaSemana.LUNES, LocalTime.of(12, 0), LocalTime.of(12, 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("La hora inicial debe ser anterior a la hora final.");
  }

  @Test
  void permiteDosRangosSeparadosEnElMismoDia() {
    Espacio espacio = espacio();
    espacio.agregarDisponibilidad(
        new Disponibilidad(espacio, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0)));

    espacio.agregarDisponibilidad(
        new Disponibilidad(espacio, DiaSemana.LUNES, LocalTime.of(14, 0), LocalTime.of(18, 0)));

    assertThat(espacio.getDisponibilidad()).hasSize(2);
  }

  @Test
  void rechazaDosRangosSuperpuestosEnElMismoDia() {
    Espacio espacio = espacio();
    espacio.agregarDisponibilidad(
        new Disponibilidad(espacio, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0)));

    assertThatThrownBy(
            () ->
                espacio.agregarDisponibilidad(
                    new Disponibilidad(
                        espacio, DiaSemana.LUNES, LocalTime.of(11, 0), LocalTime.of(13, 0))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("El rango horario se superpone con otro existente.");
  }

  private Espacio espacio() {
    return new Espacio("Cancha cubierta", null, 20, new BigDecimal("1500.00"), "FUTSAL", null);
  }
}
