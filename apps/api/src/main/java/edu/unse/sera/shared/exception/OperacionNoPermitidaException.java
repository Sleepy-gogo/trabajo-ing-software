package edu.unse.sera.shared.exception;

public class OperacionNoPermitidaException extends RuntimeException {
  public OperacionNoPermitidaException() {
    super("No tenés permiso para consultar o modificar este registro.");
  }
}
