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
  UUID id;

  // Esta es la forma correcta de definir una Foreign Key en Spring JPA
  @ManyToOne
  @JoinColumn(name = "id_espacio", referencedColumnName = "id")
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

  public Disponibilidad(DiaSemana diaSemana, LocalTime horaDesde, LocalTime horaHasta) {
    this.diaSemana=diaSemana;
    actualizarDatos(horaDesde, horaHasta);
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
    this.diaSemana = diaSemana;
    this.horaDesde = Objects.requireNonNull(horaDesde, "La hora inicial es obligatoria");
    this.horaHasta = Objects.requireNonNull(horaHasta, "La hora final es obligatoria");
  }

  /*+actualizarDisponibilidad(datos)
   *+buscarDisponibilidadEspacio(idEspacio)
   *+buscarHorariosDisponibles(fecha,espacio)
   *+consultarHorariosDisponibles(fecha,espacio)
   *+guardarDisponibilidadEspacio(datos)
   *+obtenerDisponibilidadActual(idEspacio)*/
}
