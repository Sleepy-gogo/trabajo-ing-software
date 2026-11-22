package edu.unse.sera.pagos.control;

public class MercadoPagoSolicitudRechazadaException extends RuntimeException {
  public MercadoPagoSolicitudRechazadaException(String mensaje, Throwable cause) {
    super(mensaje, cause);
  }
}
