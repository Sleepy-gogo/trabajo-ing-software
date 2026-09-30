package edu.unse.sera.reporte.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;

public class InformeNoEncontradoException extends RecursoNoEncontradoException {
  public InformeNoEncontradoException() {
    super("Informe no encontrado.");
  }
}
