package edu.unse.sera.membresia.boundary.dto;

import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifaMembresiaRequest(
    @NotNull(message = "La relación con la UNSE es obligatoria.") RelacionUnse relacionUnse,
    @NotNull(message = "El importe mensual es obligatorio.")
        @DecimalMin(value = "0.01", message = "El importe mensual debe ser mayor que cero.")
        @Digits(
            integer = 10,
            fraction = 2,
            message = "El importe mensual admite hasta 10 enteros y 2 decimales.")
        BigDecimal importeMensual,
    @NotNull(message = "La fecha de inicio de vigencia es obligatoria.") LocalDate vigenciaDesde) {}
