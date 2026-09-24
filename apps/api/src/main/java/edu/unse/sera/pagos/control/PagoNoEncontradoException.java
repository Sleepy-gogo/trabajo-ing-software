package edu.unse.sera.pagos.control;

import java.util.UUID;

public class PagoNoEncontradoException extends RuntimeException {

  public PagoNoEncontradoException(UUID id) {
    super("No se encontró el Pago con id " + id + ".");
  }
}
