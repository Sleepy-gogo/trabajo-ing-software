package edu.unse.sera.disponibilidad.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import edu.unse.sera.disponibilidad.persistence.DisponibilidadRepository;
import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

  @Mock private DisponibilidadRepository disponibilidadRepository;
  @Mock private EspacioRepository espacioRepository;

  private DisponibilidadService service;

  @BeforeEach
  void setUp() {
    service = new DisponibilidadService(disponibilidadRepository, espacioRepository);
  }

  @Test
  void registraLaDisponibilidadEnElEspacio() {
    UUID espacioId = UUID.randomUUID();
    Espacio espacio = espacio();
    when(espacioRepository.findById(espacioId)).thenReturn(Optional.of(espacio));
    when(disponibilidadRepository.save(any(Disponibilidad.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DisponibilidadDetalle detalle =
        service.registrarDisponibilidad(
            espacioId, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0));

    assertThat(detalle.diaSemana()).isEqualTo(DiaSemana.LUNES);
    assertThat(espacio.getDisponibilidad()).hasSize(1);
    verify(disponibilidadRepository).save(any(Disponibilidad.class));
  }

  @Test
  void rechazaRangosSuperpuestosParaElMismoDia() {
    UUID espacioId = UUID.randomUUID();
    when(espacioRepository.findById(espacioId)).thenReturn(Optional.of(espacio()));
    when(disponibilidadRepository
            .existsByEspacioIdAndDiaSemanaAndHoraDesdeLessThanAndHoraHastaGreaterThan(
                espacioId, DiaSemana.LUNES, LocalTime.of(12, 0), LocalTime.of(8, 0)))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service.registrarDisponibilidad(
                    espacioId, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0)))
        .isInstanceOf(DisponibilidadSuperpuestaException.class);
  }

  @Test
  void actualizaElRangoDeUnaDisponibilidadExistente() {
    UUID espacioId = UUID.randomUUID();
    UUID disponibilidadId = UUID.randomUUID();
    Disponibilidad disponibilidad =
        new Disponibilidad(espacio(), DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0));
    when(disponibilidadRepository.findByIdAndEspacioId(disponibilidadId, espacioId))
        .thenReturn(Optional.of(disponibilidad));

    DisponibilidadDetalle detalle =
        service.actualizar(espacioId, disponibilidadId, LocalTime.of(9, 0), LocalTime.of(13, 0));

    assertThat(detalle.horaDesde()).isEqualTo(LocalTime.of(9, 0));
    assertThat(detalle.horaHasta()).isEqualTo(LocalTime.of(13, 0));
  }

  private Espacio espacio() {
    return new Espacio("Cancha cubierta", null, 20, new BigDecimal("1500.00"), "FUTSAL", null);
  }
}
