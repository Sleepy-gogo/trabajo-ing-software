package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import java.util.UUID;

public class DisponibilidadDuplicadaException extends RuntimeException {

  private final DiaSemana diaSemana;
  private final UUID espacioId;

  public DisponibilidadDuplicadaException(DiaSemana diaSemana, UUID espacioId) {
    super("Ya existe una disponibilidad en el día " + diaSemana + " para el espacio " + espacioId+".");
    this.diaSemana=diaSemana;
    this.espacioId=espacioId;
  }

  public DisponibilidadDuplicadaException(DiaSemana diaSemana, UUID espacioId, Throwable cause) {
    super("Ya existe una disponibilidad en el día " + diaSemana + " para el espacio " + espacioId+".",cause);
    this.diaSemana=diaSemana;
    this.espacioId=espacioId;
  }


}
