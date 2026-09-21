package edu.unse.sera.espacio.control;

import edu.unse.sera.disponibilidad.control.DisponibilidadDetalle;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EspacioDetalle(
    UUID id,
    String nombre,
    String descripcion,
    int capacidad,
    BigDecimal tarifaHora,
    String tipo,
    String rutaImagen,
    List<DisponibilidadDetalle> disponibilidades,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    edu.unse.sera.espacio.entity.EstadoEspacio estado,
    java.util.Map<edu.unse.sera.socio.entity.RelacionUnse, BigDecimal> tarifas) {
  public EspacioDetalle(
      UUID id,
      String nombre,
      String descripcion,
      int capacidad,
      BigDecimal tarifaHora,
      String tipo,
      String rutaImagen,
      List<DisponibilidadDetalle> disponibilidades,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    this(
        id,
        nombre,
        descripcion,
        capacidad,
        tarifaHora,
        tipo,
        rutaImagen,
        disponibilidades,
        createdAt,
        updatedAt,
        edu.unse.sera.espacio.entity.EstadoEspacio.HABILITADO,
        java.util.Map.of());
  }
}
