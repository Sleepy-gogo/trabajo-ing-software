package edu.unse.sera.actividad.control;

import edu.unse.sera.actividad.entity.TipoActividad;
import java.time.OffsetDateTime;

public record ActividadDetalle(
    TipoActividad tipo, String usuario, String detalle, OffsetDateTime creadoEn) {}
