package edu.unse.sera.usuario.control;

public class UsuarioDuplicadoException extends RuntimeException {

  private final String campo;

  public UsuarioDuplicadoException(String campo) {
    super("Ya existe un usuario con ese " + campo + ".");
    this.campo = campo;
  }

  public String getCampo() {
    return campo;
  }
}
