package edu.unse.sera.reserva.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;

public class ReservaNoEncontradaException extends RecursoNoEncontradoException {
  public ReservaNoEncontradaException() {
    super("No se encontró la reserva.");
  }
}
