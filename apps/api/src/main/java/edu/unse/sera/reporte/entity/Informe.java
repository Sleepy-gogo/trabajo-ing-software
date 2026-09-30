package edu.unse.sera.reporte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "informes")
public class Informe {
  @Id private UUID id;
  private String tipo;
  private OffsetDateTime creadoEn;
  private UUID creadoPor;

  @Column(columnDefinition = "text")
  private String contenido;

  protected Informe() {}

  public Informe(UUID id, String tipo, OffsetDateTime creadoEn, UUID creadoPor, String contenido) {
    this.id = id;
    this.tipo = tipo;
    this.creadoEn = creadoEn;
    this.creadoPor = creadoPor;
    this.contenido = contenido;
  }

  public UUID getId() {
    return id;
  }

  public String getTipo() {
    return tipo;
  }

  public OffsetDateTime getCreadoEn() {
    return creadoEn;
  }

  public UUID getCreadoPor() {
    return creadoPor;
  }

  public String getContenido() {
    return contenido;
  }
}
