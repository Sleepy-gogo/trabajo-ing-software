package edu.unse.sera.disponibilidad.boundary.dto;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CrearDisponibilidadRequest(
    @NotNull(message = "El día es obligatorio.") DiaSemana diaSemana,
    @NotNull(message = "La hora inicial es obligatoria.") LocalTime horaDesde,
    @NotNull(message = "La hora final es obligatoria.") LocalTime horaHasta) {

  @AssertTrue(message = "La hora inicial debe ser anterior a la hora final.")
  public boolean isRangoValido() {
    return horaDesde == null || horaHasta == null || horaDesde.isBefore(horaHasta);
  }
}
