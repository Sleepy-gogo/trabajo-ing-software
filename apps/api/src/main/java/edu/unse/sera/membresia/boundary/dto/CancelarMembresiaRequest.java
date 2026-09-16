package edu.unse.sera.membresia.boundary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarMembresiaRequest(
    @NotBlank(message = "El motivo de cancelación es obligatorio.")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres.")
        String motivo) {}
