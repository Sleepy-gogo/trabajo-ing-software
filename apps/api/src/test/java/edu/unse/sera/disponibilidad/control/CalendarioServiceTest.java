package edu.unse.sera.disponibilidad.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.unse.sera.disponibilidad.entity.Bloqueo;
import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import edu.unse.sera.disponibilidad.persistence.BloqueoRepository;
import edu.unse.sera.disponibilidad.persistence.DisponibilidadRepository;
import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.entity.EstadoEspacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.socio.persistence.SocioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarioServiceTest {
  @Mock private EspacioRepository espacios;
  @Mock private DisponibilidadRepository horarios;
  @Mock private BloqueoRepository bloqueos;
  @Mock private SocioRepository socios;
  private CalendarioService service;
  private final UUID id = UUID.randomUUID();
  private final UUID actor = UUID.randomUUID();
  private final LocalDate fecha = LocalDate.now().plusDays(2);
  private Espacio espacio;

  @BeforeEach
  void preparar() {
    service = new CalendarioService(espacios, horarios, bloqueos, socios);
    espacio = new Espacio("Cancha", null, 20, new BigDecimal("1200.00"), "Cancha", null);
  }

  @Test
  void descuentaBloqueoYConservaAmbosExtremosLibres() {
    when(espacios.findById(id)).thenReturn(Optional.of(espacio));
    when(horarios.findAllByEspacioId(id))
        .thenReturn(
            List.of(
                new Disponibilidad(
                    espacio,
                    DiaSemana.values()[fecha.getDayOfWeek().getValue() - 1],
                    LocalTime.of(8, 0),
                    LocalTime.of(18, 0))));
    when(bloqueos.findAllByEspacioIdAndFechaOrderByDesde(id, fecha))
        .thenReturn(
            List.of(
                new Bloqueo(id, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0), "Mantenimiento")));
    var result = service.consultar(id, fecha, actor);
    assertThat(result.franjas())
        .containsExactly(
            new CalendarioService.Franja(LocalTime.of(8, 0), LocalTime.of(10, 0)),
            new CalendarioService.Franja(LocalTime.of(12, 0), LocalTime.of(18, 0)));
    assertThat(result.tarifaHora()).isEqualByComparingTo("1200");
  }

  @Test
  void mantenimientoNoOfreceHorarios() {
    espacio.cambiarEstado(EstadoEspacio.MANTENIMIENTO);
    when(espacios.findById(id)).thenReturn(Optional.of(espacio));
    assertThat(service.consultar(id, fecha, actor).franjas()).isEmpty();
    verifyNoInteractions(horarios, bloqueos);
  }

  @Test
  void rechazaBloqueosSuperpuestosConBloqueoDeFila() {
    when(espacios.bloquearPorId(id)).thenReturn(Optional.of(espacio));
    when(bloqueos.findAllByEspacioIdAndFechaOrderByDesde(id, fecha))
        .thenReturn(
            List.of(new Bloqueo(id, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0), "Limpieza")));
    assertThatThrownBy(
            () -> service.bloquear(id, fecha, LocalTime.of(11, 0), LocalTime.of(13, 0), "Otro"))
        .isInstanceOf(IllegalStateException.class);
  }
}
