package edu.unse.sera.membresia.boundary.dto;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MembresiaResponse(
    UUID id,
    UUID socioId,
    UUID nivelMembresiaId,
    String nivelMembresiaNombre,
    EstadoMembresia estado,
    LocalDate fechaAlta,
    LocalDate fechaBaja,
    LocalDate proximoVencimiento,
    BigDecimal cargoMensual) {}
