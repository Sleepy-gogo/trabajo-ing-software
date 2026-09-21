package edu.unse.sera.membresia.boundary.dto;

import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record GuardarNivelMembresiaRequest(
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,
    @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
        String descripcion,
    @NotEmpty(message = "Debe indicar al menos un beneficio.")
        List<@NotBlank @Size(max = 255) String> beneficios,
    boolean disponibleParaContratar,
    @NotEmpty(message = "Debe indicar al menos un precio por relación.")
        Map<
                @NotNull(message = "La relación con la UNSE es obligatoria.") RelacionUnse,
                @NotNull(message = "El importe mensual es obligatorio.")
                @DecimalMin(value = "0.01", message = "El importe mensual debe ser mayor que cero.")
                @Digits(
                    integer = 8,
                    fraction = 2,
                    message = "El importe mensual admite hasta 8 enteros y 2 decimales.")
                BigDecimal>
            preciosPorRelacion) {}
