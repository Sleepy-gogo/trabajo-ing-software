package edu.unse.sera.encuesta.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "encuestas")
public class Encuesta {
  @Id private UUID id = UUID.randomUUID();
  private String titulo;
  private String descripcion;
  private UUID espacioId;
  private LocalDate desde;
  private LocalDate hasta;
  private boolean activa = true;
  private OffsetDateTime creadaEn = OffsetDateTime.now(java.time.ZoneOffset.UTC);

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "encuesta_id", nullable = false)
  @OrderBy("orden ASC")
  private List<Pregunta> preguntas = new ArrayList<>();

  protected Encuesta() {}

  public Encuesta(
      String titulo,
      String descripcion,
      UUID espacioId,
      LocalDate desde,
      LocalDate hasta,
      List<Pregunta> preguntas) {
    if (titulo == null
        || titulo.isBlank()
        || titulo.trim().length() > 200
        || descripcion == null
        || descripcion.length() > 1000
        || desde == null
        || hasta == null
        || desde.isAfter(hasta)
        || preguntas == null
        || preguntas.isEmpty()
        || preguntas.size() > 20) {
      throw new IllegalArgumentException(
          "Indicá título, período y entre 1 y 20 preguntas válidas.");
    }
    this.titulo = titulo.trim();
    this.descripcion = descripcion.trim();
    this.espacioId = espacioId;
    this.desde = desde;
    this.hasta = hasta;
    this.preguntas = new ArrayList<>(preguntas);
  }

  public boolean disponible(LocalDate hoy) {
    return activa && !hoy.isBefore(desde) && !hoy.isAfter(hasta);
  }

  public void cambiarActiva(boolean valor) {
    activa = valor;
  }

  public UUID getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public UUID getEspacioId() {
    return espacioId;
  }

  public LocalDate getDesde() {
    return desde;
  }

  public LocalDate getHasta() {
    return hasta;
  }

  public boolean isActiva() {
    return activa;
  }

  public OffsetDateTime getCreadaEn() {
    return creadaEn;
  }

  public List<Pregunta> getPreguntas() {
    return List.copyOf(preguntas);
  }
}
