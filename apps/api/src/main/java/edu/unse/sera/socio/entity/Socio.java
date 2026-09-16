package edu.unse.sera.socio.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Datos específicos de socio que complementan a un usuario registrado. */
public class Socio {

  // TODO(TRA-26): Mapear esta clase como entidad JPA después de integrar el modelo de Usuario.
  // La relación debe apuntar al usuario existente; no hay que copiar nombre, DNI ni email aquí.
  private UUID id;
  private UUID usuarioId;
  private RelacionUnse relacionUnse;
  private EstadoVerificacionUnse estadoVerificacionUnse;
  private String identificadorUnse;
  private OffsetDateTime creadoEn;
  private OffsetDateTime actualizadoEn;

  // TODO(TRA-26): Agregar constructor de dominio, getters y métodos para declarar y verificar la
  // relación. La verificación debe conservar responsable, fecha y motivo en la solución de
  // auditoría.
}
