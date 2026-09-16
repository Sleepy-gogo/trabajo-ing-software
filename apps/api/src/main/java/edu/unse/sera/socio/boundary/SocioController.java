package edu.unse.sera.socio.boundary;

/** Definición pendiente del boundary HTTP administrativo de socios. */
public final class SocioController {

  private SocioController() {}

  // TODO(TRA-26): Registrar POST /api/socios cuando el alta de socio y membresía sea transaccional.

  // TODO(TRA-28): Registrar GET /api/socios con búsqueda y filtros implementados en Control.

  // TODO(TRA-26): Registrar GET /api/socios/{id} cuando pueda combinar Usuario, Socio y la
  // membresía vigente sin exponer entidades JPA.

  // TODO(TRA-27): Registrar PUT /api/socios/{id} cuando pueda validar transiciones. Guardar motivo,
  // responsable y fecha.
}
