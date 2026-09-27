package edu.unse.sera.pagos.boundary.dto;

import edu.unse.sera.pagos.control.SuscripcionMercadoPagoDetalle;
import java.util.UUID;

public record SuscripcionResponse(
    UUID id, String preapprovalId, String estado, String checkoutUrl) {
  public static SuscripcionResponse from(SuscripcionMercadoPagoDetalle detalle) {
    return new SuscripcionResponse(
        detalle.id(), detalle.preapprovalId(), detalle.estado(), detalle.checkoutUrl());
  }
}
