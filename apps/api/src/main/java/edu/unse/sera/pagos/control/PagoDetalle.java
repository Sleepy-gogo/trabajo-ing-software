package edu.unse.sera.pagos.control;

import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import java.math.BigDecimal;
import java.util.UUID;

public record PagoDetalle(
  UUID id, ConceptoPago conceptoPago, UUID idUsuario, EstadoPago estado, MedioPago medioPago, BigDecimal monto,
  String comprobante, UUID idMembresia) {}
