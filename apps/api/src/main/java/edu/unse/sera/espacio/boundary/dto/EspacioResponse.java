package edu.unse.sera.espacio.boundary.dto;

import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import java.math.BigDecimal;
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
    List<Disponibilidad> disponibilidadList) {}
