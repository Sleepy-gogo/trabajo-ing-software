package edu.unse.sera.membresia.boundary.dto;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TarifaMembresiaResponse(
    UUID id,
    RelacionUnse relacionUnse,
    BigDecimal importeMensual,
    LocalDate vigenciaDesde,
    LocalDate vigenciaHasta) {}
