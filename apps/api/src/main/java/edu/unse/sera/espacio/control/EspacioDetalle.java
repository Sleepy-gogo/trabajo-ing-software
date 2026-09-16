package edu.unse.sera.espacio.control;

import edu.unse.sera.disponibilidad.entity.Disponibilidad;
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
    List<Disponibilidad> disponibilidadList,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
