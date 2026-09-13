package edu.unse.sera.usuario.boundary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
    @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 200, message = "El nombre completo no puede superar los 200 caracteres.")
        String nombreCompleto,
    @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
        String email,
    @Min(value = 1, message = "El DNI debe ser mayor que cero.")
        @Max(value = 99_999_999, message = "El DNI no puede superar los 8 dígitos.")
        int dni) {}
