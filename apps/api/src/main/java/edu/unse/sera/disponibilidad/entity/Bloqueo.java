package edu.unse.sera.disponibilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bloqueos_espacios")
public class Bloqueo {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID espacioId;

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(nullable = false)
  private LocalTime desde;

  @Column(nullable = false)
  private LocalTime hasta;

  @Column(nullable = false, length = 500)
  private String motivo;

  protected Bloqueo() {}

  public Bloqueo(UUID espacioId, LocalDate fecha, LocalTime desde, LocalTime hasta, String motivo) {
    if (fecha == null
        || desde == null
        || hasta == null
        || !desde.isBefore(hasta)
        || motivo == null
        || motivo.isBlank()
        || motivo.length() > 500) {
      throw new IllegalArgumentException(
          "Indicá una fecha, un rango horario válido y un motivo de hasta 500 caracteres.");
    }
    this.espacioId = espacioId;
    this.fecha = fecha;
    this.desde = desde;
    this.hasta = hasta;
    this.motivo = motivo.trim();
  }

  public UUID getId() {
    return id;
  }

  public UUID getEspacioId() {
    return espacioId;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public LocalTime getDesde() {
    return desde;
  }

  public LocalTime getHasta() {
    return hasta;
  }

  public String getMotivo() {
    return motivo;
  }
}
