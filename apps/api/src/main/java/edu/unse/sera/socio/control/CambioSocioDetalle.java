package edu.unse.sera.socio.control;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CambioSocioDetalle(
    UUID id, UUID responsableId, String motivo, String detalle, OffsetDateTime fecha) {}
