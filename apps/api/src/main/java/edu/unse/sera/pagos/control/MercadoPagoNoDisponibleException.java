package edu.unse.sera.pagos.control;

public class MercadoPagoNoDisponibleException extends RuntimeException {
  public MercadoPagoNoDisponibleException() {
    super("No se pudo consultar Mercado Pago. Intentá de nuevo.");
  }

  public MercadoPagoNoDisponibleException(Throwable cause) {
    super("No se pudo consultar Mercado Pago. Intentá de nuevo.", cause);
  }
}
