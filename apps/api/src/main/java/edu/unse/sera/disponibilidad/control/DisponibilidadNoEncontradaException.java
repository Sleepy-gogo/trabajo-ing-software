package edu.unse.sera.disponibilidad.control;

import java.util.UUID;

public class DisponibilidadNoEncontradaException extends RuntimeException {

  public DisponibilidadNoEncontradaException(UUID id) {
    super("No existe la disponibilidad con id " + id + ".");
  }
}
