package edu.unse.sera.pagos.entity;

public class EstadoPagoInvalidoException extends RuntimeException {

  public EstadoPagoInvalidoException(String message) {
    super(message);
  }
}
