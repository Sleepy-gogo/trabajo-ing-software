package edu.unse.sera.encuesta.control;

import edu.unse.sera.encuesta.entity.TipoPregunta;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EncuestaDatos(
    String titulo,
    String descripcion,
    UUID espacioId,
    LocalDate desde,
    LocalDate hasta,
    List<PreguntaDatos> preguntas) {
  public record PreguntaDatos(
      String texto, TipoPregunta tipo, boolean obligatoria, List<String> opciones) {}
}
