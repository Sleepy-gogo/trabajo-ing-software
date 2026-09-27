package edu.unse.sera.pagos.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VencimientoPendientes {

  private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");

  private final PagoRepository pagos;
  private final MembresiaRepository membresias;
  private final SuscripcionMercadoPagoRepository suscripciones;

  public VencimientoPendientes(
      PagoRepository pagos,
      MembresiaRepository membresias,
      SuscripcionMercadoPagoRepository suscripciones) {
    this.pagos = pagos;
    this.membresias = membresias;
    this.suscripciones = suscripciones;
  }

  @Scheduled(cron = "0 0 * * * *", zone = "UTC")
  @Transactional
  public void revisar() {
    procesar(OffsetDateTime.now(ZoneOffset.UTC));
  }

  void procesar(OffsetDateTime ahora) {
    OffsetDateTime limite = ahora.minusHours(1);
    pagos
        .findAllByEstadoAndCreatedAtBefore(EstadoPago.PENDIENTE, limite)
        .forEach(
            pago -> {
              if (!suscripciones.existsByPagoInicialIdAndEstadoNot(pago.getId(), "canceled")) {
                pago.cancelarPendiente();
              }
            });
    pagos.flush();
    membresias
        .findAllByEstadoAndUpdatedAtBefore(EstadoMembresia.PENDIENTE_PAGO, limite)
        .forEach(
            membresia -> {
              if (!pagos.existsByMembresiaIdAndEstado(membresia.getId(), EstadoPago.PENDIENTE)) {
                membresia.cancelar(LocalDate.ofInstant(ahora.toInstant(), ZONA_NEGOCIO));
              }
            });
  }
}
