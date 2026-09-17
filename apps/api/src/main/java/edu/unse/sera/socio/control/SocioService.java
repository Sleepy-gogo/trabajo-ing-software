package edu.unse.sera.socio.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.persistence.SocioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Casos de uso administrativos para socios y sus membresías. */
@Service
public class SocioService {

  private final SocioRepository socioRepository;

  public SocioService(SocioRepository socioRepository) {
    this.socioRepository=socioRepository;
  }

  // TODO(TRA-26): Convertir en @Service e inyectar repositories por constructor. Registrar el
  // socio
  // y la membresía pendiente en una sola transacción.
  public SocioDetalle registrar(
      UUID usuarioId, RelacionUnse relacionUnse, String identificadorUnse, UUID nivelMembresiaId) {
    throw pendiente();
  }

  // TODO(TRA-28): Buscar por nombre, DNI o email y aplicar los filtros solo cuando estén
  // presentes.
  public List<SocioDetalle> listar(
      String criterio, EstadoMembresia estadoMembresia, RelacionUnse relacionUnse) {
    throw pendiente();
  }

  // TODO(TRA-26): Lanzar una excepción concreta si el socio no existe.
  public SocioDetalle obtener(UUID socioId) {
    throw pendiente();
  }

  // TODO(TRA-27): Validar nivel y transiciones de estado antes de guardar. Registrar quién hizo el
  // cambio cuando el modelo de autenticación exponga al usuario responsable.
  public SocioDetalle actualizar(
      UUID socioId,
      RelacionUnse relacionUnse,
      EstadoVerificacionUnse estadoVerificacionUnse,
      String identificadorUnse,
      UUID nivelMembresiaId,
      EstadoMembresia estadoMembresia,
      String motivo) {
    throw pendiente();
  }

  private UnsupportedOperationException pendiente() {
    return new UnsupportedOperationException(
        "El incremento de socios todavía no está implementado.");
  }

  public SocioDetalle toResponse() {
    return new SocioDetalle();
  }
}
