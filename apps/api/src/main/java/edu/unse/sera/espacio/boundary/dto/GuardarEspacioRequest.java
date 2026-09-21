package edu.unse.sera.espacio.boundary.dto;

import jakarta.validation.constraints.DecimalMin;
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
    @Min(value = 1, message = "La capacidad debe ser mayor que cero.") int capacidad,
    @NotNull(message = "La tarifa por hora es obligatoria.")
        @DecimalMin(value = "0.00", message = "La tarifa por hora no puede ser negativa.")
        BigDecimal tarifaHora,
    @NotBlank(message = "El tipo es obligatorio.")
        @Size(max = 100, message = "El tipo no puede superar los 100 caracteres.")
        String tipo,
    @Size(max = 255, message = "La ruta de imagen no puede superar los 255 caracteres.")
        String rutaImagen) {}
