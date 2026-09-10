package edu.unse.sera.usuario.control;

public class UsuarioNoEncontradoException extends RuntimeException {

  public UsuarioNoEncontradoException(String message) {
    super("No existe el usuario con " + message + ".");
  }

}
