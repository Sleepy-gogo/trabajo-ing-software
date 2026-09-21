package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import java.util.UUID;

public class DisponibilidadSuperpuestaException extends RuntimeException {

  public DisponibilidadSuperpuestaException(DiaSemana diaSemana, UUID espacioId) {
    super(
        "El rango horario se superpone con otro de "
            + diaSemana
            + " en el espacio "
            + espacioId
            + ".");
  }
}
