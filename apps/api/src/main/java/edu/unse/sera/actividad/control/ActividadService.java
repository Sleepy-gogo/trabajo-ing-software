package edu.unse.sera.actividad.control;

import edu.unse.sera.actividad.entity.RegistroActividad;
import edu.unse.sera.actividad.entity.TipoActividad;
import edu.unse.sera.actividad.persistence.RegistroActividadRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActividadService {
  private final RegistroActividadRepository registros;

  public ActividadService(RegistroActividadRepository registros) {
    this.registros = registros;
  }

  /**
   * Guarda la acción en una transacción propia. Así queda registrado también un intento de inicio
   * de sesión rechazado, aunque la operación que lo originó termine con una excepción.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrar(UUID usuarioId, String email, TipoActividad tipo, String detalle) {
    registros.save(
        new RegistroActividad(usuarioId, email, tipo, detalle, OffsetDateTime.now(ZoneOffset.UTC)));
  }

  @Transactional(readOnly = true)
  public List<ActividadDetalle> recientes() {
    return registros.findTop8ByOrderByCreadoEnDesc().stream()
        .map(r -> new ActividadDetalle(r.getTipo(), r.getEmail(), r.getDetalle(), r.getCreadoEn()))
        .toList();
  }
}
