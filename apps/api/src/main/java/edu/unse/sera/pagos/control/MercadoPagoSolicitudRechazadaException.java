package edu.unse.sera.pagos.control;

/** El proveedor rechazó el alta; no existe una suscripción remota que recuperar. */
public class MercadoPagoSolicitudRechazadaException extends RuntimeException {
  public MercadoPagoSolicitudRechazadaException(String mensaje, Throwable cause) {
    super(mensaje, cause);
  }
}
