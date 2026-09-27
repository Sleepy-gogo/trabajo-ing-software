package edu.unse.sera.membresia.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VencimientoMembresias {

  private final MembresiaRepository membresias;

  public VencimientoMembresias(MembresiaRepository membresias) {
    this.membresias = membresias;
  }

  @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
  @Transactional
  public void revisar() {
    procesar(LocalDate.now(ZoneOffset.UTC));
  }

  void procesar(LocalDate hoy) {
    membresias
        .findAllByEstadoAndProximoVencimientoBefore(EstadoMembresia.ACTIVA, hoy)
        .forEach(membresia -> membresia.actualizarPorVencimiento(hoy));
    membresias
        .findAllByEstado(EstadoMembresia.VENCIDA)
        .forEach(membresia -> membresia.actualizarPorVencimiento(hoy));
  }
}
