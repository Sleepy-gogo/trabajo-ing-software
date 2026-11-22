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
    service =
        new CalendarioService(
            espacios,
            horarios,
            bloqueos,
            socios,
            org.mockito.Mockito.mock(edu.unse.sera.reserva.persistence.ReservaRepository.class));
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

  @Test
  void hoyNoOfreceHorariosQueEmpiezanEnMenosDeCuarentaMinutos() {
    LocalDate hoy = LocalDate.of(2026, 11, 19);
    abiertoTodoElDia();
    // A las 10:00:30 el primer inicio admitido es 10:41 (40 minutos, redondeado al minuto).
    var franjas = service.consultar(id, hoy, actor, instante(hoy, 10, 0, 30)).franjas();
    assertThat(franjas).containsExactly(new CalendarioService.Franja(LocalTime.of(10, 41), FIN));
  }

  @Test
  void mananaOfreceTodosLosHorariosLibres() {
    LocalDate hoy = LocalDate.of(2026, 11, 19);
    abiertoTodoElDia();
    var franjas = service.consultar(id, hoy.plusDays(1), actor, instante(hoy, 10, 0, 0)).franjas();
    assertThat(franjas).containsExactly(new CalendarioService.Franja(INICIO, FIN));
  }

  @Test
  void cercaDeMedianocheNoOfreceHorariosDelDiaQueTermina() {
    LocalDate hoy = LocalDate.of(2026, 11, 19);
    abiertoTodoElDia();
    var ahora = instante(hoy, 23, 40, 0);
    assertThat(service.consultar(id, hoy, actor, ahora).franjas()).isEmpty();
    assertThat(service.consultar(id, hoy.plusDays(1), actor, ahora).franjas())
        .containsExactly(new CalendarioService.Franja(LocalTime.of(0, 20), FIN));
    assertThat(service.consultar(id, hoy.minusDays(1), actor, ahora).franjas()).isEmpty();
  }

  private static final LocalTime INICIO = LocalTime.MIN;
  private static final LocalTime FIN = LocalTime.of(23, 0);

  private void abiertoTodoElDia() {
    when(espacios.findById(id)).thenReturn(Optional.of(espacio));
    when(horarios.findAllByEspacioId(id))
        .thenReturn(
            java.util.Arrays.stream(DiaSemana.values())
                .map(d -> new Disponibilidad(espacio, d, INICIO, FIN))
                .toList());
  }

  private static java.time.OffsetDateTime instante(
      LocalDate fecha, int hora, int minuto, int segundo) {
    return fecha
        .atTime(hora, minuto, segundo)
        .atZone(edu.unse.sera.reserva.entity.Reserva.ZONA)
        .toOffsetDateTime();
  }
}
