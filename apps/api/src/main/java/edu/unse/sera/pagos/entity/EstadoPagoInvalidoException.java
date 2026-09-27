package edu.unse.sera.pagos.entity;

public class EstadoPagoInvalidoException extends IllegalStateException {

  public EstadoPagoInvalidoException(String message) {
    super(message);
  }
}
