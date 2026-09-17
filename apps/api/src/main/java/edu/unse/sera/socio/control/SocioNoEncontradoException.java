package edu.unse.sera.socio.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.util.UUID;

public class SocioNoEncontradoException extends RecursoNoEncontradoException {

  public SocioNoEncontradoException(UUID id) {
    super("No existe el socio con id " + id + ".");
  }
}
