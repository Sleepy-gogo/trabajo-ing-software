package edu.unse.sera.pagos.control;

import java.math.BigDecimal;

public record FacturaMercadoPago(
    long id,
    String preapprovalId,
    String referencia,
    String moneda,
    BigDecimal monto,
    long paymentId) {}
