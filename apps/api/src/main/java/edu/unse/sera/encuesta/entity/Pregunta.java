package edu.unse.sera.encuesta.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "preguntas_encuesta")
public class Pregunta {
  @Id private UUID id = UUID.randomUUID();
  private String texto;

  @Enumerated(EnumType.STRING)
  private TipoPregunta tipo;

  private boolean obligatoria;
  private int orden;

  @ElementCollection
  @CollectionTable(name = "opciones_pregunta", joinColumns = @JoinColumn(name = "pregunta_id"))
  @OrderColumn(name = "orden")
  @Column(name = "valor")
  private List<String> opciones = new ArrayList<>();

  protected Pregunta() {}

  public Pregunta(
      String texto, TipoPregunta tipo, boolean obligatoria, List<String> opciones, int orden) {
    if (texto == null || texto.isBlank() || texto.trim().length() > 500 || tipo == null) {
      throw new IllegalArgumentException("Cada pregunta necesita texto y tipo válidos.");
    }
    this.texto = texto.trim();
    this.tipo = tipo;
    this.obligatoria = obligatoria;
    this.orden = orden;
    this.opciones =
        new ArrayList<>(
            opciones == null
                ? List.of()
                : opciones.stream().map(o -> o == null ? "" : o.trim()).toList());
    if (tipo == TipoPregunta.OPCION) {
      if (this.opciones.size() < 2
          || this.opciones.size() > 10
          || this.opciones.stream().anyMatch(o -> o.isEmpty() || o.length() > 200)
          || this.opciones.stream().distinct().count() != this.opciones.size()) {
        throw new IllegalArgumentException(
            "La pregunta de opción necesita entre 2 y 10 opciones distintas.");
      }
    } else if (!this.opciones.isEmpty()) {
      throw new IllegalArgumentException("Solo las preguntas de opción admiten opciones.");
    }
  }

  public String validarRespuesta(String valor) {
    String respuesta = valor == null ? "" : valor.trim();
    if (respuesta.isEmpty()) {
      if (obligatoria) {
        throw new IllegalArgumentException("Completá la pregunta obligatoria: " + texto);
      }
      return respuesta;
    }
    if (respuesta.length() > 2000
        || (tipo == TipoPregunta.CALIFICACION
            && !List.of("1", "2", "3", "4", "5").contains(respuesta))
        || (tipo == TipoPregunta.OPCION && !opciones.contains(respuesta))) {
      throw new IllegalArgumentException("Respuesta inválida para: " + texto);
    }
    return respuesta;
  }

  public UUID getId() {
    return id;
  }

  public String getTexto() {
    return texto;
  }

  public TipoPregunta getTipo() {
    return tipo;
  }

  public boolean isObligatoria() {
    return obligatoria;
  }

  public List<String> getOpciones() {
    return List.copyOf(opciones);
  }
}
