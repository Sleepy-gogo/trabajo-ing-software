package edu.unse.sera.membresia.control;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de una tarifa sin depender de los DTO de Boundary. */
public record TarifaMembresiaDatos(
    RelacionUnse relacionUnse, BigDecimal importeMensual, LocalDate vigenciaDesde) {}
