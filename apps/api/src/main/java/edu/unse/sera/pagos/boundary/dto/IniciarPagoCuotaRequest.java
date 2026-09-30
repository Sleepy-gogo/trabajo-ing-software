package edu.unse.sera.pagos.boundary.dto;

import edu.unse.sera.pagos.entity.MedioPago;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IniciarPagoCuotaRequest(
    @NotNull(message = "La membresía es obligatoria.") UUID membresiaId,
    @NotNull(message = "El medio de pago es obligatorio.") MedioPago medioPago) {}
