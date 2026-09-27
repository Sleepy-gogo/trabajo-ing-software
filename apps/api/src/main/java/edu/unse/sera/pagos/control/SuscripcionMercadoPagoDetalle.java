package edu.unse.sera.pagos.control;

import java.util.UUID;

public record SuscripcionMercadoPagoDetalle(
    UUID id, String preapprovalId, String estado, String checkoutUrl) {}
