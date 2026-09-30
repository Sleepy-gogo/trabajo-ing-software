package edu.unse.sera.pagos.control;

/** Estado de cobro de la contratación actual, independiente del historial paginado. */
public record CobroMembresiaDetalle(
    PagoDetalle pagoPendiente, SuscripcionMercadoPagoDetalle suscripcion) {}
