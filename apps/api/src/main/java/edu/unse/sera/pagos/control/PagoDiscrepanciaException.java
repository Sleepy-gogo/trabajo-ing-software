package edu.unse.sera.pagos.control;

public class PagoDiscrepanciaException extends RuntimeException {

  public PagoDiscrepanciaException() {
    super("El pago no corresponde con la sesión activa");
  }
}
