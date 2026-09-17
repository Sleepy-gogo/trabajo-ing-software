package edu.unse.sera.disponibilidad.entity;

import edu.unse.sera.espacio.entity.Espacio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "disponibilidades")
public class Disponibilidad {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_espacio", nullable = false)
  private Espacio espacio;

  @Enumerated(EnumType.STRING)
  @Column(name = "dia_semana", nullable = false, length = 10)
  private DiaSemana diaSemana;

  @Column(name = "hora_desde", nullable = false)
  private LocalTime horaDesde;

  @Column(name = "hora_hasta", nullable = false)
  private LocalTime horaHasta;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Disponibilidad() {}

  public Disponibilidad(
      Espacio espacio, DiaSemana diaSemana, LocalTime horaDesde, LocalTime horaHasta) {
    this.espacio = Objects.requireNonNull(espacio, "El espacio es obligatorio.");
    this.diaSemana = Objects.requireNonNull(diaSemana, "El día es obligatorio.");
    actualizarDatos(horaDesde, horaHasta);
  }

  public UUID getId() {
    return id;
  }

  public DiaSemana getDiaSemana() {
    return diaSemana;
  }

  public LocalTime getHoraDesde() {
    return horaDesde;
  }

  public LocalTime getHoraHasta() {
    return horaHasta;
  }

  public Espacio getEspacio() {
    return espacio;
  }

  public void actualizarDatos(LocalTime horaDesde, LocalTime horaHasta) {
    LocalTime desde = Objects.requireNonNull(horaDesde, "La hora inicial es obligatoria.");
    LocalTime hasta = Objects.requireNonNull(horaHasta, "La hora final es obligatoria.");
    if (!desde.isBefore(hasta)) {
      throw new IllegalArgumentException("La hora inicial debe ser anterior a la hora final.");
    }
    this.horaDesde = desde;
    this.horaHasta = hasta;
  }
}
