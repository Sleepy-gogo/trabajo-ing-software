package edu.unse.sera.encuesta.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;

public class EncuestaNoEncontradaException extends RecursoNoEncontradoException {
  public EncuestaNoEncontradaException() {
    super("Encuesta no encontrada.");
  }
}
