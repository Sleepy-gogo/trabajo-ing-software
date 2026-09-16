package edu.unse.sera.membresia.boundary;

/** Definición pendiente del boundary HTTP de membresías. */
public final class MembresiaController {

  private MembresiaController() {}

  // TODO(TRA-26): Registrar POST /api/membresias cuando contratar() tenga implementación completa.
  // Debe responder 201 con Location y crear la membresía en PENDIENTE_PAGO.

  // TODO(TRA-26): Registrar GET /api/membresias/{id} cuando consultar el estado sea un caso de uso
  // funcional y los recursos inexistentes se traduzcan a 404.

  // TODO(TRA-27): Registrar POST /api/membresias/{id}/cancelacion después de implementar y probar
  // las transiciones de estado. El cliente confirma la acción, pero Control protege la regla.
}
