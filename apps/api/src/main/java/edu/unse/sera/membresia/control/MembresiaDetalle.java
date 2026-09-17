package edu.unse.sera.membresia.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import java.time.LocalDate;
import java.util.UUID;

public record MembresiaDetalle(
    UUID id,
    UUID socioId,
    UUID nivelMembresiaId,
    String nivelMembresiaNombre,
    EstadoMembresia estado,
    LocalDate fechaAlta,
    LocalDate fechaBaja,
    LocalDate proximoVencimiento) {}
