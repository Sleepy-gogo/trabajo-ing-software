package edu.unse.sera.usuario.control;

public class PasswordInvalidaException extends RuntimeException {
  public PasswordInvalidaException() {
    super("La contraseña supera los 72 bytes. Usá menos caracteres.");
  }
}
