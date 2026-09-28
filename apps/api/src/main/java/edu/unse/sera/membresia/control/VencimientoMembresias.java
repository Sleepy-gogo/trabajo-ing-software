package edu.unse.sera.membresia.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VencimientoMembresias {

  private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");

  private final MembresiaRepository membresias;

  public VencimientoMembresias(MembresiaRepository membresias) {
    this.membresias = membresias;
  }

  @Scheduled(cron = "0 5 0 * * *", zone = "America/Argentina/Buenos_Aires")
  @Transactional
  public void revisar() {
    procesar(LocalDate.now(ZONA_NEGOCIO));
  }

  void procesar(LocalDate hoy) {
    membresias
        .findAllByEstadoAndProximoVencimientoBefore(EstadoMembresia.ACTIVA, hoy.plusDays(1))
        .forEach(membresia -> membresia.actualizarPorVencimiento(hoy));
    membresias
        .findAllByEstado(EstadoMembresia.VENCIDA)
        .forEach(membresia -> membresia.actualizarPorVencimiento(hoy));
  }
}
