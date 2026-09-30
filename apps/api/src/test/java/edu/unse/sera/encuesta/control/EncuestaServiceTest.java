package edu.unse.sera.encuesta.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.encuesta.entity.Encuesta;
import edu.unse.sera.encuesta.entity.EnvioEncuesta;
import edu.unse.sera.encuesta.entity.Pregunta;
import edu.unse.sera.encuesta.entity.TipoPregunta;
import edu.unse.sera.encuesta.persistence.EncuestaRepository;
import edu.unse.sera.encuesta.persistence.EnvioEncuestaRepository;
import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.reserva.entity.EstadoReserva;
import edu.unse.sera.reserva.entity.Reserva;
import edu.unse.sera.reserva.persistence.ReservaRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.usuario.entity.Usuario;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EncuestaServiceTest {
  private final EncuestaRepository encuestas = mock(EncuestaRepository.class);
  private final EnvioEncuestaRepository envios = mock(EnvioEncuestaRepository.class);
  private final ReservaRepository reservas = mock(ReservaRepository.class);
  private final EncuestaService service =
      new EncuestaService(encuestas, envios, reservas, mock(EspacioRepository.class));
  private final UUID actor = UUID.randomUUID();
  private final UUID reservaId = UUID.randomUUID();
  private final Pregunta pregunta =
      new Pregunta("Experiencia", TipoPregunta.CALIFICACION, true, List.of(), 0);
  private Encuesta encuesta;
  private Reserva reserva;

  @BeforeEach
  void preparar() {
    var hoy = LocalDate.now(Reserva.ZONA);
    encuesta =
        new Encuesta(
            "Visita", "Feedback", null, hoy.minusDays(1), hoy.plusDays(1), List.of(pregunta));
    var usuario = mock(Usuario.class);
    var espacio = mock(Espacio.class);
    reserva = mock(Reserva.class);
    when(usuario.getId()).thenReturn(actor);
    when(espacio.getId()).thenReturn(UUID.randomUUID());
    when(reserva.getUsuario()).thenReturn(usuario);
    when(reserva.getEspacio()).thenReturn(espacio);
    when(reserva.getId()).thenReturn(reservaId);
    when(reserva.getEstado()).thenReturn(EstadoReserva.CONFIRMADA);
    when(reserva.getConsumidaEn()).thenReturn(OffsetDateTime.now());
    when(encuestas.bloquear(encuesta.getId())).thenReturn(Optional.of(encuesta));
    when(reservas.findById(reservaId)).thenReturn(Optional.of(reserva));
    when(envios.findByEncuestaIdAndReservaId(encuesta.getId(), reservaId))
        .thenReturn(Optional.empty());
  }

  @Test
  void guardaRespuestasValidadasConLaReserva() {
    service.responder(encuesta.getId(), reservaId, actor, Map.of(pregunta.getId(), " 4 "));
    var guardado = ArgumentCaptor.forClass(EnvioEncuesta.class);
    verify(envios).saveAndFlush(guardado.capture());
    assertThat(guardado.getValue().getRespuestas()).containsEntry(pregunta.getId(), "4");
    assertThat(guardado.getValue().getReservaId()).isEqualTo(reservaId);
  }

  @Test
  void rechazaOtraCuentaSinGuardar() {
    assertThatThrownBy(
            () ->
                service.responder(
                    encuesta.getId(), reservaId, UUID.randomUUID(), Map.of(pregunta.getId(), "4")))
        .isInstanceOf(OperacionNoPermitidaException.class);
    verify(envios, never()).saveAndFlush(any());
  }

  @Test
  void rechazaReservasSinUsoEncuestasCerradasYDuplicados() {
    when(reserva.getConsumidaEn()).thenReturn(null);
    assertThatThrownBy(() -> service.responder(encuesta.getId(), reservaId, actor, Map.of()))
        .isInstanceOf(IllegalStateException.class);
    when(reserva.getConsumidaEn()).thenReturn(OffsetDateTime.now());
    encuesta.cambiarActiva(false);
    assertThatThrownBy(() -> service.responder(encuesta.getId(), reservaId, actor, Map.of()))
        .isInstanceOf(IllegalStateException.class);
    encuesta.cambiarActiva(true);
    when(envios.findByEncuestaIdAndReservaId(encuesta.getId(), reservaId))
        .thenReturn(Optional.of(new EnvioEncuesta(encuesta.getId(), reservaId, Map.of())));
    assertThatThrownBy(() -> service.responder(encuesta.getId(), reservaId, actor, Map.of()))
        .isInstanceOf(IllegalStateException.class);
    verify(envios, never()).saveAndFlush(any());
  }

  @Test
  void rechazaPreguntasAjenasYObligatoriasVacias() {
    assertThatThrownBy(
            () ->
                service.responder(
                    encuesta.getId(), reservaId, actor, Map.of(UUID.randomUUID(), "4")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.responder(encuesta.getId(), reservaId, actor, Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(envios, never()).saveAndFlush(any());
  }
}
