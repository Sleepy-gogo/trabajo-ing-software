package edu.unse.sera.membresia.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import java.util.List;
import java.util.UUID;

/** Casos de uso de niveles y membresías definidos en CU-05 a CU-09. */
public class MembresiaService {

  // TODO(TRA-25): Convertir en @Service e inyectar repositories por constructor.
  public List<NivelMembresiaDetalle> listarNiveles(boolean soloDisponibles) {
    throw pendiente();
  }

  // TODO(TRA-25): Devolver el plan y sus precios por relación UNSE.
  public NivelMembresiaDetalle obtenerNivel(UUID nivelMembresiaId) {
    throw pendiente();
  }

  // TODO(TRA-28): Crear el nivel y sus precios en una transacción; rechazar nombres repetidos.
  public NivelMembresiaDetalle crearNivel(NivelMembresiaDatos datos) {
    throw pendiente();
  }

  // TODO(TRA-28): Actualizar el nivel sin alterar membresías existentes.
  public NivelMembresiaDetalle actualizarNivel(UUID nivelMembresiaId, NivelMembresiaDatos datos) {
    throw pendiente();
  }

  // TODO(TRA-28): Dar de baja lógica al nivel. No borrar registros usados por membresías.
  public void deshabilitarNivel(UUID nivelMembresiaId) {
    throw pendiente();
  }

  // TODO(TRA-26): Rechazar niveles inactivos y membresías incompatibles. Crear la membresía en
  // PENDIENTE_PAGO; el módulo de pagos resolverá el precio y la activará cuando reciba un
  // resultado aprobado.
  public MembresiaDetalle contratar(UUID socioId, UUID nivelMembresiaId) {
    throw pendiente();
  }

  // TODO(TRA-26): Consultar por id y devolver nivel, estado y fechas. El importe viene de Cuota.
  public MembresiaDetalle obtener(UUID membresiaId) {
    throw pendiente();
  }

  // TODO(TRA-27): Validar transiciones y guardar motivo, responsable y fecha para auditoría. El
  // módulo de pagos resolverá el precio de la próxima cuota según el nivel actualizado.
  public MembresiaDetalle actualizar(
      UUID membresiaId, UUID nivelMembresiaId, EstadoMembresia estado, String motivo) {
    throw pendiente();
  }

  // TODO(TRA-27): Permitir cancelación solo desde los estados acordados. Coordinar la baja de un
  // pago recurrente sin perder la solicitud de cancelación.
  public MembresiaDetalle cancelar(UUID membresiaId, String motivo) {
    throw pendiente();
  }

  private UnsupportedOperationException pendiente() {
    return new UnsupportedOperationException(
        "El incremento de membresías todavía no está implementado.");
  }
}
