package edu.unse.sera.shared.exception;

public abstract class RecursoNoEncontradoException extends RuntimeException {

  protected RecursoNoEncontradoException(String message) {
    super(message);
  }
}
