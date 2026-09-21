package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.util.UUID;

public class DisponibilidadNoEncontradaException extends RecursoNoEncontradoException {

  public DisponibilidadNoEncontradaException(UUID id) {
    super("No existe la disponibilidad con id " + id + ".");
  }
}
