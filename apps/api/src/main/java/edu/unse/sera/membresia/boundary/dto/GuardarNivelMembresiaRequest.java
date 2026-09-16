package edu.unse.sera.membresia.boundary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record GuardarNivelMembresiaRequest(
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,
    @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
        String descripcion,
    @NotNull(message = "El importe mensual base es obligatorio.")
        @DecimalMin(value = "0.01", message = "El importe mensual base debe ser mayor que cero.")
        @Digits(
            integer = 10,
            fraction = 2,
            message = "El importe mensual base admite hasta 10 enteros y 2 decimales.")
        BigDecimal importeMensualBase,
    @NotEmpty(message = "Debe indicar al menos un beneficio.") List<@NotBlank String> beneficios,
    List<@NotBlank String> serviciosIncluidos,
    List<@NotBlank String> condiciones,
    boolean disponibleParaContratar,
    @Valid @NotEmpty(message = "Debe indicar al menos una tarifa.")
        List<TarifaMembresiaRequest> tarifas) {}
