package edu.unse.sera.membresia.boundary.dto;

import edu.unse.sera.pagos.entity.MedioPago;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContratarMembresiaRequest(
    @NotNull(message = "El socio es obligatorio.") UUID socioId,
    @NotNull(message = "El nivel de membresía es obligatorio.") UUID nivelMembresiaId,
    @NotNull(message = "El medio de pago es obligatorio.") MedioPago medioPago) {}
