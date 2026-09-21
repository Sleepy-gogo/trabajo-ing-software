package edu.unse.sera.socio.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import java.time.LocalDate;
import java.util.UUID;

/** Proyección de lectura que combina usuario, socio, nivel y membresía. */
public record SocioDetalle(
    UUID id,
    UUID usuarioId,
    String nombreCompleto,
    String email,
    int dni,
    RelacionUnse relacionUnse,
    EstadoVerificacionUnse estadoVerificacionUnse,
    String identificadorUnse,
    UUID membresiaId,
    UUID nivelMembresiaId,
    String nivelMembresiaNombre,
    EstadoMembresia estadoMembresia,
    LocalDate proximoVencimiento) {}
