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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pagos")
public class Pago {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private ConceptoPago concepto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoPago estado;

  @Enumerated(EnumType.STRING)
  @Column(name = "medio_pago", nullable = false, updatable = false)
  private MedioPago medioPago;

  @Column(nullable = false, precision = 12, scale = 2, updatable = false)
  private BigDecimal monto;

  private String comprobante;

  @Column(name = "mercado_pago_payment_id", unique = true, length = 100)
  private String mercadoPagoPaymentId;

  @Column(name = "mercado_pago_factura_id")
  private Long mercadoPagoFacturaId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membresia_id", updatable = false)
  private Membresia membresia;

  private UUID reservaId;
  private String checkoutUrl;

  @Column(name = "contratacion_id", updatable = false)
  private UUID contratacionId;

  @Column(name = "clave_solicitud", nullable = false, updatable = false)
  private UUID claveSolicitud;

  @Column(name = "relacion_aplicada", nullable = false, updatable = false, length = 30)
  private String relacionAplicada;

  @Column(name = "nivel_nombre_aplicado", nullable = false, updatable = false, length = 255)
  private String nivelNombreAplicado;

  @Column(name = "aprobado_en")
  private OffsetDateTime aprobadoEn;

  @Column(name = "aplicado_en")
  private OffsetDateTime aplicadoEn;

  @Column(name = "vencimiento_anterior")
  private LocalDate vencimientoAnterior;

  @Column(name = "vencimiento_resultante")
  private LocalDate vencimientoResultante;

  @Column(name = "requiere_revision", nullable = false)
  private boolean requiereRevision;

  @Column(name = "motivo_revision", length = 500)
  private String motivoRevision;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version private long version;

  protected Pago() {}

  public Pago(
      ConceptoPago concepto,
      Usuario usuario,
      MedioPago medioPago,
      BigDecimal monto,
      Membresia membresia) {
    this(concepto, usuario, medioPago, monto, membresia, UUID.randomUUID());
  }

  public Pago(
      ConceptoPago concepto,
      Usuario usuario,
      MedioPago medioPago,
      BigDecimal monto,
      Membresia membresia,
      UUID claveSolicitud) {
    if (concepto != ConceptoPago.CUOTA_MENSUAL || membresia == null) {
      throw new IllegalArgumentException("Solo se admiten renovaciones de membresía.");
    }
    if (monto == null
        || monto.signum() <= 0
        || monto.scale() > 2
        || monto.compareTo(new BigDecimal("99999999.99")) > 0) {
      throw new IllegalArgumentException("El importe del pago es inválido.");
    }
    this.concepto = concepto;
    this.usuario = Objects.requireNonNull(usuario);
    this.medioPago = Objects.requireNonNull(medioPago);
    this.monto = monto;
    this.membresia = membresia;
    this.contratacionId = Objects.requireNonNull(membresia.getContratacionId());
    this.claveSolicitud = Objects.requireNonNull(claveSolicitud);
    this.relacionAplicada = membresia.getSocio().relacionParaTarifa().name();
    this.nivelNombreAplicado = membresia.getNivelMembresia().getNombre();
    this.estado = EstadoPago.PENDIENTE;
  }

  public Pago(edu.unse.sera.reserva.entity.Reserva reserva, MedioPago medio) {
    if (reserva.importeAPagar().signum() <= 0) {
      throw new IllegalArgumentException("La reserva no requiere pago.");
    }
    this.concepto =
        reserva.getCreditoAplicado().signum() > 0
            ? ConceptoPago.DIFERENCIA_TICKET
            : ConceptoPago.RESERVA;
    this.usuario = reserva.getUsuario();
    this.reservaId = Objects.requireNonNull(reserva.getId());
    this.medioPago = Objects.requireNonNull(medio);
    this.monto = reserva.importeAPagar();
    this.claveSolicitud = UUID.randomUUID();
    this.relacionAplicada = reserva.getRelacionAplicada();
    this.nivelNombreAplicado = reserva.getEspacio().getNombre();
    this.estado = EstadoPago.PENDIENTE;
  }

  /** Las cuotas recurrentes conservan la contratación y tarifa de la suscripción original. */
  public Pago nuevaCuotaSuscripcion() {
    if (concepto != ConceptoPago.CUOTA_MENSUAL || medioPago != MedioPago.MERCADO_PAGO) {
      throw new IllegalStateException("El pago no pertenece a una suscripción mensual.");
    }
    Pago cuota = new Pago(concepto, usuario, medioPago, monto, membresia);
    cuota.contratacionId = contratacionId;
    cuota.relacionAplicada = relacionAplicada;
    cuota.nivelNombreAplicado = nivelNombreAplicado;
    return cuota;
  }

  public UUID getReservaId() {
    return reservaId;
  }

  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public void vincularCheckout(String url) {
    checkoutUrl = Objects.requireNonNull(url);
  }

  public boolean aprobar(String identidad, OffsetDateTime fecha) {
    if (identidad == null || identidad.isBlank() || fecha == null) {
      throw new IllegalArgumentException("La aprobación requiere identidad y fecha.");
    }
    if (estado == EstadoPago.APROBADO) {
      if (!identidad.equals(comprobante)) {
        throw new EstadoPagoInvalidoException("El pago ya tiene otro comprobante.");
      }
      return false;
    }
    if (estado != EstadoPago.PENDIENTE
        && !(medioPago == MedioPago.MERCADO_PAGO
            && (estado == EstadoPago.CANCELADO || estado == EstadoPago.RECHAZADO))) {
      throw new EstadoPagoInvalidoException("El estado del pago es inválido para aprobar.");
    }
    comprobante = identidad;
    aprobadoEn = fecha;
    estado = EstadoPago.APROBADO;
    return true;
  }

  public void marcarAplicado(LocalDate anterior, LocalDate nuevo, OffsetDateTime instante) {
    if (estado != EstadoPago.APROBADO || aplicadoEn != null || nuevo == null || instante == null) {
      throw new EstadoPagoInvalidoException("La renovación ya fue aplicada o no está aprobada.");
    }
    vencimientoAnterior = anterior;
    vencimientoResultante = nuevo;
    aplicadoEn = instante;
  }

  public void marcarParaRevision(String motivo) {
    if (estado != EstadoPago.APROBADO || motivo == null || motivo.isBlank()) {
      throw new EstadoPagoInvalidoException("La revisión requiere un pago aprobado y un motivo.");
    }
    requiereRevision = true;
    motivoRevision = motivo;
  }

  public void rechazar() {
    if (estado == EstadoPago.RECHAZADO) {
      return;
    }
    if (estado != EstadoPago.PENDIENTE) {
      throw new EstadoPagoInvalidoException("Solo se puede rechazar un pago pendiente.");
    }
    estado = EstadoPago.RECHAZADO;
  }

  public void cancelarPendiente() {
    if (estado == EstadoPago.CANCELADO) {
      return;
    }
    if (estado != EstadoPago.PENDIENTE) {
      throw new EstadoPagoInvalidoException("Solo se puede cancelar un pago pendiente.");
    }
    estado = EstadoPago.CANCELADO;
  }

  public void vincularMercadoPagoPaymentId(String paymentId) {
    if (medioPago != MedioPago.MERCADO_PAGO || paymentId == null || paymentId.isBlank()) {
      throw new IllegalArgumentException("El identificador de Mercado Pago es inválido.");
    }
    if (mercadoPagoPaymentId != null && !mercadoPagoPaymentId.equals(paymentId)) {
      throw new EstadoPagoInvalidoException("El pago ya está vinculado a otro cobro.");
    }
    mercadoPagoPaymentId = paymentId;
  }

  public void vincularFacturaMercadoPago(long facturaId) {
    if (medioPago != MedioPago.MERCADO_PAGO
        || facturaId <= 0
        || (mercadoPagoFacturaId != null && mercadoPagoFacturaId != facturaId)) {
      throw new IllegalArgumentException("La factura de Mercado Pago es inválida.");
    }
    mercadoPagoFacturaId = facturaId;
  }

  public UUID getId() {
    return id;
  }

  public ConceptoPago getConcepto() {
    return concepto;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public EstadoPago getEstado() {
    return estado;
  }

  public MedioPago getMedioPago() {
    return medioPago;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public String getComprobante() {
    return comprobante;
  }

  public String getMercadoPagoPaymentId() {
    return mercadoPagoPaymentId;
  }

  public Long getMercadoPagoFacturaId() {
    return mercadoPagoFacturaId;
  }

  public Membresia getMembresia() {
    return membresia;
  }

  public UUID getContratacionId() {
    return contratacionId;
  }

  public UUID getClaveSolicitud() {
    return claveSolicitud;
  }

  public String getRelacionAplicada() {
    return relacionAplicada;
  }

  public String getNivelNombreAplicado() {
    return nivelNombreAplicado;
  }

  public OffsetDateTime getAprobadoEn() {
    return aprobadoEn;
  }

  public OffsetDateTime getAplicadoEn() {
    return aplicadoEn;
  }

  public LocalDate getVencimientoAnterior() {
    return vencimientoAnterior;
  }

  public LocalDate getVencimientoResultante() {
    return vencimientoResultante;
  }

  public boolean isRequiereRevision() {
    return requiereRevision;
  }

  public String getMotivoRevision() {
    return motivoRevision;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
