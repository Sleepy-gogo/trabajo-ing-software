package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VencimientoPendientesTest {

  @Mock private PagoRepository pagos;
  @Mock private MembresiaRepository membresias;
  @Mock private SuscripcionMercadoPagoRepository suscripciones;

  @Test
  void cancelaPagoYSolicitudSinPagoPendiente() {
    OffsetDateTime ahora = OffsetDateTime.of(2026, 9, 27, 2, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime limite = ahora.minusHours(1);
    Pago pago = new Pago(null, null, null, null, null);
    Membresia membresia = new Membresia(null, null);
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(membresia, "id", id);
    when(pagos.findAllByEstadoAndCreatedAtBefore(EstadoPago.PENDIENTE, limite))
        .thenReturn(List.of(pago));
    when(membresias.findAllByEstadoAndUpdatedAtBefore(EstadoMembresia.PENDIENTE_PAGO, limite))
        .thenReturn(List.of(membresia));

    new VencimientoPendientes(pagos, membresias, suscripciones).procesar(ahora);

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.CANCELADO);
    assertThat(membresia.getEstado()).isEqualTo(EstadoMembresia.CANCELADA);
    assertThat(membresia.getFechaBaja()).isEqualTo(LocalDate.of(2026, 9, 26));
  }

  @Test
  void conservaSolicitudSiTieneUnPagoPendienteMasReciente() {
    OffsetDateTime ahora = OffsetDateTime.of(2026, 9, 27, 12, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime limite = ahora.minusHours(1);
    Membresia membresia = new Membresia(null, null);
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(membresia, "id", id);
    when(membresias.findAllByEstadoAndUpdatedAtBefore(EstadoMembresia.PENDIENTE_PAGO, limite))
        .thenReturn(List.of(membresia));
    when(pagos.existsByMembresiaIdAndEstado(id, EstadoPago.PENDIENTE)).thenReturn(true);

    new VencimientoPendientes(pagos, membresias, suscripciones).procesar(ahora);

    assertThat(membresia.getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
  }

  @Test
  void conservaPrimerPagoMientrasLaSuscripcionSigueVigente() {
    OffsetDateTime ahora = OffsetDateTime.of(2026, 9, 27, 12, 0, 0, 0, ZoneOffset.UTC);
    Pago pago = new Pago(null, null, null, null, null);
    UUID pagoId = UUID.randomUUID();
    ReflectionTestUtils.setField(pago, "id", pagoId);
    when(pagos.findAllByEstadoAndCreatedAtBefore(EstadoPago.PENDIENTE, ahora.minusHours(1)))
        .thenReturn(List.of(pago));
    when(suscripciones.existsByPagoInicialIdAndEstadoNot(pagoId, "canceled")).thenReturn(true);

    new VencimientoPendientes(pagos, membresias, suscripciones).procesar(ahora);

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
  }
}
