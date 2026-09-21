package edu.unse.sera.socio.boundary.dto;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ActualizarSocioRequest(
    @NotNull(message = "La relación con la UNSE es obligatoria.") RelacionUnse relacionUnse,
    @NotNull(message = "El estado de verificación es obligatorio.")
        EstadoVerificacionUnse estadoVerificacionUnse,
    @Size(max = 50, message = "El identificador UNSE no puede superar los 50 caracteres.")
        String identificadorUnse,
    UUID nivelMembresiaId,
    EstadoMembresia estadoMembresia,
    @NotBlank(message = "El motivo del cambio es obligatorio.")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres.")
        String motivo) {}
