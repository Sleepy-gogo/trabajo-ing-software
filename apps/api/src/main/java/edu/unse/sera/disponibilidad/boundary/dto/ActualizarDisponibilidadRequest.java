package edu.unse.sera.disponibilidad.boundary.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ActualizarDisponibilidadRequest(
    @NotNull(message = "La hora inicial es obligatoria.") LocalTime horaDesde,
    @NotNull(message = "La hora final es obligatoria.") LocalTime horaHasta) {

  @AssertTrue(message = "La hora inicial debe ser anterior a la hora final.")
  public boolean isRangoValido() {
    return horaDesde == null || horaHasta == null || horaDesde.isBefore(horaHasta);
  }
}
