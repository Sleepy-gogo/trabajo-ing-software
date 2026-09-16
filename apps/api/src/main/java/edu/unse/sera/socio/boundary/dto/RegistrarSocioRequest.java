package edu.unse.sera.socio.boundary.dto;

import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegistrarSocioRequest(
    @NotNull(message = "El usuario es obligatorio.") UUID usuarioId,
    @NotNull(message = "La relación con la UNSE es obligatoria.") RelacionUnse relacionUnse,
    @Size(max = 50, message = "El identificador UNSE no puede superar los 50 caracteres.")
        String identificadorUnse,
    @NotNull(message = "El nivel de membresía es obligatorio.") UUID nivelMembresiaId) {}
