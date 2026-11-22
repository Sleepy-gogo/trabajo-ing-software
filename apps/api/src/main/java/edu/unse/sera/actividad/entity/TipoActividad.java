package edu.unse.sera.actividad.entity;

/** Acciones que quedan registradas en el historial de actividad de los usuarios. */
public enum TipoActividad {
  INICIO_SESION,
  INICIO_SESION_FALLIDO,
  INICIO_SESION_BLOQUEADO,
  CIERRE_SESION,
  RESERVA_CREADA,
  RESERVA_CANCELADA,
  VERIFICACION_ACTUALIZADA,
  INFORME_GENERADO
}
