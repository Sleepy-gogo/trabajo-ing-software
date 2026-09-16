package edu.unse.sera.membresia.control;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TarifaMembresiaDetalle(
    UUID id,
    RelacionUnse relacionUnse,
    BigDecimal importeMensual,
    LocalDate vigenciaDesde,
    LocalDate vigenciaHasta) {}
