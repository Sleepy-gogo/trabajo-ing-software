package edu.unse.sera.usuario.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.util.UUID;

public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

  public UsuarioNoEncontradoException(UUID id) {
    super("No existe el usuario con id " + id + ".");
  }
}
