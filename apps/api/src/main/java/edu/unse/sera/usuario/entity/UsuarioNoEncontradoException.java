package edu.unse.sera.usuario.entity;

import java.util.UUID;

public class UsuarioNoEncontradoException extends RuntimeException {
  public UsuarioNoEncontradoException(String message) {
    super(message);
  }

  public UsuarioNoEncontradoException(UUID id) {
    super("No existe el usuario con id " + id + ".");
  }
}
