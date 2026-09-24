package edu.unse.sera.pagos.entity;

import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.usuario.entity.Usuario;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pagos")
public class Pago {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  ConceptoPago concepto;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false, referencedColumnName = "id")
  Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  EstadoPago estado;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  MedioPago medioPago;

  @Column(nullable = false)
  BigDecimal monto;

  @Column()
  String comprobante;


  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "membresia_id", referencedColumnName = "id")
  Membresia membresia;

  // TODO: DESCOMENTAR CUANDO SE CREE LA CLASE RESERVA
//  @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, optional = true)
//  @JoinColumn(name = "reserva_id", referencedColumnName = "id")
//  Reserva reserva;

  /*@Column()
  *LocalDateTime fechaEmisionComprobante;*/

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;


  public Pago(ConceptoPago concepto, Usuario usuario, EstadoPago estado, MedioPago medioPago,
    BigDecimal monto, Membresia membresia) {
    this.concepto = concepto;
    this.usuario = usuario;
    this.estado = estado;
    this.medioPago = medioPago;
    this.monto = monto;
    this.membresia = membresia;
  }

  public void aprobar(String comprobante) {
    if (estado == EstadoPago.APROBADO) return;

    if (estado != EstadoPago.PENDIENTE) {
      throw new EstadoPagoInvalidoException("El estado del pago es inválido para aprobar");
    }
    setComprobante(comprobante);
    setEstado(EstadoPago.APROBADO);
  }

  public void rechazar() {
    if (estado != EstadoPago.PENDIENTE) return;

    setEstado(EstadoPago.RECHAZADO);
  }

  public UUID getId() {
    return id;
  }

  public ConceptoPago getConcepto() {
    return concepto;
  }

  public void setConcepto(ConceptoPago concepto) {
    this.concepto = concepto;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public EstadoPago getEstado() {
    return estado;
  }

  public void setEstado(EstadoPago estado) {
    this.estado = estado;
  }

  public MedioPago getMedioPago() {
    return medioPago;
  }

  public void setMedioPago(MedioPago medioPago) {
    this.medioPago = medioPago;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public void setMonto(BigDecimal monto) {
    this.monto = monto;
  }

  public String getComprobante() {
    return comprobante;
  }

  public void setComprobante(String comprobante) {
    this.comprobante = comprobante;
  }

  public Membresia getMembresia() {
    return membresia;
  }

  public void setMembresia(Membresia membresia) {
    this.membresia = membresia;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

}
