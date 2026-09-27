package edu.unse.sera.membresia.entity;

import edu.unse.sera.socio.entity.Socio;
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

  @OneToOne(fetch = FetchType.LAZY, optional = false)
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

  @Column(name = "proximo_vencimiento")
  private LocalDate proximoVencimiento;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @jakarta.persistence.Version private long version;

  protected Membresia() {}

  public Membresia(Socio socio, NivelMembresia nivelMembresia) {
    this.socio = socio;
    this.nivelMembresia = nivelMembresia;
    this.estado = EstadoMembresia.PENDIENTE_PAGO;
    this.fechaAlta = LocalDate.now();
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

  public void setProximoVencimiento(LocalDate nuevaFecha) {
    this.proximoVencimiento = nuevaFecha;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void cambiarEstado(EstadoMembresia nuevo) {
    transicionarA(nuevo, false, false);
  }

  private void transicionarA(EstadoMembresia nuevo, boolean pagoAprobado, boolean nuevaSolicitud) {
    if (nuevo == null) {
      throw new IllegalArgumentException("El estado es obligatorio.");
    }
    if (estado == nuevo) {
      return;
    }
    if (!puedeTransicionarA(nuevo, pagoAprobado, nuevaSolicitud)) {
      throw new IllegalStateException(
          "La transición de "
              + estado
              + " a "
              + nuevo
              + " no está permitida. La activación requiere un pago aprobado.");
    }
    estado = nuevo;
    if (nuevo == EstadoMembresia.CANCELADA) {
      fechaBaja = LocalDate.now();
    }
  }

  private boolean puedeTransicionarA(
      EstadoMembresia nuevo, boolean pagoAprobado, boolean nuevaSolicitud) {
    boolean permitido =
        switch (estado) {
          case PENDIENTE_PAGO ->
              nuevo == EstadoMembresia.CANCELADA
                  || (nuevo == EstadoMembresia.ACTIVA && pagoAprobado);
          case ACTIVA ->
              nuevo == EstadoMembresia.VENCIDA
                  || nuevo == EstadoMembresia.SUSPENDIDA
                  || nuevo == EstadoMembresia.CANCELADA;
          case VENCIDA ->
              nuevo == EstadoMembresia.SUSPENDIDA
                  || nuevo == EstadoMembresia.CANCELADA
                  || (nuevo == EstadoMembresia.ACTIVA && pagoAprobado);
          case SUSPENDIDA ->
              nuevo == EstadoMembresia.CANCELADA
                  || (nuevo == EstadoMembresia.ACTIVA && pagoAprobado);
          case CANCELADA -> nuevo == EstadoMembresia.PENDIENTE_PAGO && nuevaSolicitud;
        };
    return permitido;
  }

  public boolean admitePago() {
    return estado == EstadoMembresia.ACTIVA
        || puedeTransicionarA(EstadoMembresia.ACTIVA, true, false);
  }

  public boolean tieneBeneficios() {
    return estado == EstadoMembresia.ACTIVA;
  }

  public void actualizarPorVencimiento(LocalDate hoy) {
    if (proximoVencimiento == null || !proximoVencimiento.isBefore(hoy)) {
      return;
    }
    if (estado == EstadoMembresia.ACTIVA) {
      cambiarEstado(EstadoMembresia.VENCIDA);
    }
    if (estado == EstadoMembresia.VENCIDA && !proximoVencimiento.plusMonths(2).isAfter(hoy)) {
      cambiarEstado(EstadoMembresia.SUSPENDIDA);
    }
  }

  public void cancelar() {
    cambiarEstado(EstadoMembresia.CANCELADA);
  }

  public void cancelar(LocalDate fecha) {
    cancelar();
    fechaBaja = fecha;
  }

  public void activarPorPago() {
    transicionarA(EstadoMembresia.ACTIVA, true, false);
  }

  public void renovarSolicitud(NivelMembresia nivel) {
    if (estado != EstadoMembresia.CANCELADA) {
      throw new IllegalStateException("Ya existe una membresía vigente o pendiente.");
    }
    setNivelMembresia(nivel);
    transicionarA(EstadoMembresia.PENDIENTE_PAGO, false, true);
    fechaAlta = LocalDate.now();
    fechaBaja = null;
    proximoVencimiento = null;
  }

  public void setNivelMembresia(NivelMembresia nivel) {
    if (nivel == null || !nivel.isDisponibleParaContratar()) {
      throw new IllegalArgumentException("El nivel no está disponible para contratar.");
    }
    this.nivelMembresia = nivel;
  }
}
