package edu.unse.sera.pagos.boundary.dto;

import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import java.math.BigDecimal;
import java.util.UUID;

public record PagoResponse(
    UUID id,
    ConceptoPago concepto,
    UUID usuarioId,
    EstadoPago estado,
    MedioPago medioPago,
    BigDecimal monto,
    String comprobante,
    UUID membresiaId) {}
