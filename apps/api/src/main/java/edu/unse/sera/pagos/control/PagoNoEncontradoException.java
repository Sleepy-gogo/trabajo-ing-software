package edu.unse.sera.pagos.control;

import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.util.UUID;

public class PagoNoEncontradoException extends RecursoNoEncontradoException {

  public PagoNoEncontradoException(UUID id) {
    super("No se encontró el Pago con id " + id + ".");
  }
}
