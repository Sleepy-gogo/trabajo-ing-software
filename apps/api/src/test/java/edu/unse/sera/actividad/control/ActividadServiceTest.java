package edu.unse.sera.actividad.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.actividad.entity.RegistroActividad;
import edu.unse.sera.actividad.entity.TipoActividad;
import edu.unse.sera.actividad.persistence.RegistroActividadRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ActividadServiceTest {
  private final RegistroActividadRepository registros = mock(RegistroActividadRepository.class);
  private final ActividadService service = new ActividadService(registros);

  @Test
  void guardaLaAccionConElMomentoEnQueOcurre() {
    UUID usuario = UUID.randomUUID();
    var antes = OffsetDateTime.now();

    service.registrar(usuario, "ana@example.com", TipoActividad.RESERVA_CREADA, "Cancha");

    var captor = ArgumentCaptor.forClass(RegistroActividad.class);
    verify(registros).save(captor.capture());
    assertThat(captor.getValue().getUsuarioId()).isEqualTo(usuario);
    assertThat(captor.getValue().getTipo()).isEqualTo(TipoActividad.RESERVA_CREADA);
    assertThat(captor.getValue().getCreadoEn()).isAfterOrEqualTo(antes);
  }

  @Test
  void devuelveLasUltimasAcciones() {
    var registro =
        new RegistroActividad(
            null,
            "x@example.com",
            TipoActividad.INICIO_SESION_FALLIDO,
            "Contraseña incorrecta",
            OffsetDateTime.now());
    when(registros.findTop8ByOrderByCreadoEnDesc()).thenReturn(List.of(registro));

    assertThat(service.recientes())
        .containsExactly(
            new ActividadDetalle(
                TipoActividad.INICIO_SESION_FALLIDO,
                "x@example.com",
                "Contraseña incorrecta",
                registro.getCreadoEn()));
  }
}
