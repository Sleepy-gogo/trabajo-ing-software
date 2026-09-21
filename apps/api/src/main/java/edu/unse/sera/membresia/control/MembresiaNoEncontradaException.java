package edu.unse.sera.membresia.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;

public class MembresiaNoEncontradaException extends RecursoNoEncontradoException {
  public MembresiaNoEncontradaException() {
    super("No se encontró el nivel o la membresía solicitada.");
  }
}
