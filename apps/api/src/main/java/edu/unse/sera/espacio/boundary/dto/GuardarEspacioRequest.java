package edu.unse.sera.espacio.boundary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GuardarEspacioRequest(
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,
    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
        String descripcion,
    @Min(value = 0, message = "La capacidad debe ser un número positivo.")
        @Max(value = 99_999_999, message = "La capacidad no puede superar los 8 dígitos.")
        int capacidad,
    @NotNull(message = "La tarifa por hora es necesaria") BigDecimal tarifaHora,
    @NotNull(message = "El tipo es obligatorio.") String tipo,
    String rutaImagen) {}
