package edu.unse.sera.espacio.control;

public class EspacioDuplicadoException extends RuntimeException {

  private final String nombre;

  public EspacioDuplicadoException(String nombre) {
    super("Ya existe un espacio de nombre " + nombre + ".");
    this.nombre = nombre;
  }

  public EspacioDuplicadoException(String nombre, Throwable cause) {
    super("Ya existe un espacio de nombre " + nombre + ".", cause);
    this.nombre = nombre;
  }

  public String getCampo() {
    return nombre;
  }
}
