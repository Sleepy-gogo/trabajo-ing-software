package edu.unse.sera.pagos.control;

import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoDetalle(
    UUID id,
    ConceptoPago conceptoPago,
    UUID idUsuario,
    String titular,
    EstadoPago estado,
    MedioPago medioPago,
    BigDecimal monto,
    String comprobante,
    UUID idMembresia,
    OffsetDateTime creadoEn,
    OffsetDateTime aprobadoEn,
    OffsetDateTime aplicadoEn,
    LocalDate vencimientoResultante,
    boolean requiereRevision,
    String motivoRevision) {}
