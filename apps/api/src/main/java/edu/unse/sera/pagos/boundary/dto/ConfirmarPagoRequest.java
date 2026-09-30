package edu.unse.sera.pagos.boundary.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarPagoRequest(
    @NotBlank(message = "El comprobante es obligatorio.") String comprobante) {}
