package edu.unse.sera.pagos.control;

import edu.unse.sera.shared.exception.OperacionNoPermitidaException;

public class PagoDiscrepanciaException extends OperacionNoPermitidaException {

  public PagoDiscrepanciaException() {
    super("El pago no corresponde con la sesión activa.");
  }
}
