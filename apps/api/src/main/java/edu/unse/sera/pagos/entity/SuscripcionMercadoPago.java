package edu.unse.sera.pagos.entity;

import edu.unse.sera.membresia.entity.Membresia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "suscripciones_mercado_pago")
public class SuscripcionMercadoPago {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "membresia_id", nullable = false)
  private Membresia membresia;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pago_inicial_id", nullable = false)
  private Pago pagoInicial;

  @Column(name = "preapproval_id", unique = true, length = 100)
  private String preapprovalId;

  @Column(nullable = false, length = 30)
  private String estado;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal monto;

  @Column(name = "checkout_url", length = 1000)
  private String checkoutUrl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected SuscripcionMercadoPago() {}

  public SuscripcionMercadoPago(Membresia membresia, Pago pagoInicial) {
    this.membresia = membresia;
    this.pagoInicial = pagoInicial;
    this.estado = "pending";
    this.monto = pagoInicial.getMonto();
  }

  public void vincular(String preapprovalId, String checkoutUrl) {
    if (preapprovalId == null
        || preapprovalId.isBlank()
        || checkoutUrl == null
        || checkoutUrl.isBlank()) {
      throw new IllegalArgumentException("Mercado Pago no devolvió la suscripción y su enlace.");
    }
    this.preapprovalId = preapprovalId;
    this.checkoutUrl = checkoutUrl;
  }

  public void actualizarEstado(String nuevoEstado) {
    if (nuevoEstado == null || nuevoEstado.isBlank()) {
      throw new IllegalArgumentException("El estado de la suscripción es obligatorio.");
    }
    this.estado = nuevoEstado;
  }

  public UUID getId() {
    return id;
  }

  public Membresia getMembresia() {
    return membresia;
  }

  public Pago getPagoInicial() {
    return pagoInicial;
  }

  public String getPreapprovalId() {
    return preapprovalId;
  }

  public String getEstado() {
    return estado;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public String getCheckoutUrl() {
    return checkoutUrl;
  }
}
