package edu.unse.sera.espacio.boundary.dto;

import edu.unse.sera.disponibilidad.boundary.dto.DisponibilidadResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EspacioResponse(
    UUID id,
    String nombre,
    String descripcion,
    int capacidad,
    BigDecimal tarifaHora,
    String tipo,
    String rutaImagen,
    List<DisponibilidadResponse> disponibilidades,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn) {}
