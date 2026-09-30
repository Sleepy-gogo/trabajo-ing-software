package edu.unse.sera.encuesta.control;

import edu.unse.sera.encuesta.entity.TipoPregunta;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EncuestaDetalle(
    UUID id,
    String titulo,
    String descripcion,
    UUID espacioId,
    LocalDate desde,
    LocalDate hasta,
    boolean activa,
    OffsetDateTime creadaEn,
    List<PreguntaDetalle> preguntas) {
  public record PreguntaDetalle(
      UUID id, String texto, TipoPregunta tipo, boolean obligatoria, List<String> opciones) {}

  public record Asignacion(
      EncuestaDetalle encuesta,
      UUID reservaId,
      String espacio,
      LocalDate fecha,
      String estado,
      Map<UUID, String> respuestas,
      OffsetDateTime enviadaEn) {}

  public record Resultado(
      UUID envioId,
      UUID reservaId,
      String titular,
      String espacio,
      LocalDate fecha,
      OffsetDateTime enviadaEn,
      Map<UUID, String> respuestas) {}

  public record Estadistica(
      UUID preguntaId,
      String texto,
      long respuestas,
      Double promedio,
      Map<String, Long> distribucion) {}

  public record Resultados(
      EncuestaDetalle encuesta,
      int total,
      List<Estadistica> estadisticas,
      List<Resultado> envios) {}
}
