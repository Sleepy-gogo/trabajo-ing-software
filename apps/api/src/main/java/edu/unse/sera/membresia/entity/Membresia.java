package edu.unse.sera.membresia.entity;

import edu.unse.sera.socio.entity.Socio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Membresía contratada por un socio. Los importes pertenecen a las cuotas y pagos. */
@Entity
@Table(name = "membresias")
public class Membresia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "socio_id", nullable = false)
  private Socio socio;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "nivel_membresia_id", nullable = false)
  private NivelMembresia nivelMembresia;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoMembresia estado;

  @Column(nullable = false, name = "fecha_alta")
  private LocalDate fechaAlta;

  @Column(name = "fecha_baja")
  private LocalDate fechaBaja;

  @Column(name = "proximo_vencimiento", nullable = false)
  private LocalDate proximoVencimiento;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Membresia(Socio socio, NivelMembresia nivelMembresia) {
    this.socio = socio;
    this.nivelMembresia = nivelMembresia;
    this.estado = EstadoMembresia.PENDIENTE_PAGO;
  }

  public UUID getId() {
    return id;
  }

  public Socio getSocio() {
    return socio;
  }

  public NivelMembresia getNivelMembresia() {
    return nivelMembresia;
  }

  public EstadoMembresia getEstado() {
    return estado;
  }

  public LocalDate getFechaAlta() {
    return fechaAlta;
  }

  public LocalDate getFechaBaja() {
    return fechaBaja;
  }

  public LocalDate getProximoVencimiento() {
    return proximoVencimiento;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  // Pendiente -> Activa
  public void activar(LocalDate proximoVencimiento) {
    if (this.estado != EstadoMembresia.PENDIENTE_PAGO && this.estado != EstadoMembresia.CANCELADA) {
      // throw error de Estado.
      return;
    }
    this.fechaAlta = LocalDate.now();
    this.estado = EstadoMembresia.ACTIVA;
    setProximoVencimiento(proximoVencimiento);
  }

  // Activa -> Cancelada
  public void cancelar() {
    if (this.estado != EstadoMembresia.ACTIVA) {
      // throw error de Estado.
      return;
    }
    this.fechaBaja = LocalDate.now();
    this.estado = EstadoMembresia.CANCELADA;
  }

  // Activa -> Vencida
  public void marcarVencimiento() {
    if (this.estado != EstadoMembresia.ACTIVA) {
      // throw error de Estado.
      return;
    }
    this.estado = EstadoMembresia.VENCIDA;
  }

  // Vencida -> suspendida
  public void suspender() {
    if (this.estado != EstadoMembresia.VENCIDA) {
      // throw error de Estado.
      return;
    }
    this.fechaBaja = LocalDate.now();
    this.estado = EstadoMembresia.SUSPENDIDA;
  }

  // Suspendida -> Activa
  public void reactivar(LocalDate proximoVencimiento) {
    if (this.estado != EstadoMembresia.SUSPENDIDA) {
      // throw error de Estado.
      return;
    }
    this.fechaAlta = LocalDate.now();
    this.estado = EstadoMembresia.ACTIVA;
    setProximoVencimiento(proximoVencimiento);
  }

  public void setProximoVencimiento(LocalDate proximoVencimiento) {
    this.proximoVencimiento = proximoVencimiento;
  }

  public void setNivelMembresia(NivelMembresia nivelMembresia) {
    this.nivelMembresia = nivelMembresia;
  }
}
