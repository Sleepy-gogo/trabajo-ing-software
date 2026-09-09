package edu.unse.sera.espacio.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.util.UUID;

public class EspacioNoEncontradoException extends RecursoNoEncontradoException {

  public EspacioNoEncontradoException(UUID id) {
    super("No existe el espacio con id " + id + ".");
  }
}
