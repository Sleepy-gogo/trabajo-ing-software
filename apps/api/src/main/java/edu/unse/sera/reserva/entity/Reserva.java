package edu.unse.sera.reserva.entity;

import edu.unse.sera.espacio.entity.Espacio;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {
  public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "espacio_id")
  private Espacio espacio;

  private LocalDate fecha;
  private LocalTime desde;
  private LocalTime hasta;
  private int personas;

  @Column(precision = 12, scale = 2)
  private BigDecimal tarifaHora;

  private String relacionAplicada;

  @Column(precision = 12, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  private EstadoReserva estado = EstadoReserva.PENDIENTE_PAGO;

  private String codigo;
  private OffsetDateTime venceEn;
  private OffsetDateTime creadaEn;
  private UUID ticketOrigenId;

  @Column(precision = 12, scale = 2)
  private BigDecimal creditoAplicado = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2)
  private BigDecimal saldoTicket = BigDecimal.ZERO;

  private UUID claveSolicitud;
  @Version private long version;

  protected Reserva() {}

  public Reserva(
      Usuario usuario,
      Espacio espacio,
      LocalDate fecha,
      LocalTime desde,
      LocalTime hasta,
      int personas,
      BigDecimal tarifa,
      String relacion,
      UUID clave,
      OffsetDateTime ahora) {
    validarHorario(fecha, desde, hasta, ahora);
    if (personas < 1 || personas > espacio.getCapacidad()) {
      throw new IllegalArgumentException(
          "La cantidad de personas supera la capacidad del espacio.");
    }
    this.usuario = usuario;
    this.espacio = espacio;
    this.fecha = fecha;
    this.desde = desde;
    this.hasta = hasta;
    this.personas = personas;
    this.tarifaHora = tarifa;
    this.relacionAplicada = relacion;
    this.total = tarifa.multiply(BigDecimal.valueOf(Duration.between(desde, hasta).toHours()));
    if (total.compareTo(new BigDecimal("99999999.99")) > 0) {
      throw new IllegalArgumentException("El importe de la reserva supera el máximo permitido.");
    }
    this.claveSolicitud = clave;
    this.creadaEn = ahora;
    OffsetDateTime inicio = fecha.atTime(desde).atZone(ZONA).toOffsetDateTime();
    this.venceEn = inicio.isBefore(ahora.plusHours(1)) ? inicio : ahora.plusHours(1);
  }

  public static void validarHorario(
      LocalDate fecha, LocalTime desde, LocalTime hasta, OffsetDateTime ahora) {
    if (fecha == null
        || desde == null
        || hasta == null
        || !desde.isBefore(hasta)
        || desde.getSecond() != 0
        || hasta.getSecond() != 0
        || desde.getNano() != 0
        || hasta.getNano() != 0
        || Duration.between(desde, hasta).toMinutes() % 60 != 0
        || !fecha.atTime(desde).atZone(ZONA).toInstant().isAfter(ahora.toInstant())) {
      throw new IllegalArgumentException(
          "Elegí un horario futuro, con duración en horas completas y dentro del mismo día.");
    }
  }

  public void aplicarTicket(Reserva origen) {
    if (ticketOrigenId != null
        || estado != EstadoReserva.PENDIENTE_PAGO
        || origen.estado != EstadoReserva.CANCELADA
        || origen.saldoTicket.signum() <= 0
        || !origen.usuario.getId().equals(usuario.getId())) {
      throw new IllegalStateException("El ticket no está disponible para esta reserva.");
    }
    creditoAplicado = origen.saldoTicket.min(total);
    origen.saldoTicket = origen.saldoTicket.subtract(creditoAplicado);
    ticketOrigenId = origen.id;
  }

  public void confirmar(OffsetDateTime ahora) {
    if (estado == EstadoReserva.CONFIRMADA) {
      return;
    }
    if (estado != EstadoReserva.PENDIENTE_PAGO || !venceEn.isAfter(ahora)) {
      throw new IllegalStateException("La reserva ya no admite confirmación.");
    }
    estado = EstadoReserva.CONFIRMADA;
    codigo = "SERA-" + UUID.randomUUID();
  }

  public void cancelar(OffsetDateTime ahora, boolean vencimiento) {
    if (estado == EstadoReserva.CANCELADA || estado == EstadoReserva.VENCIDA) {
      return;
    }
    if (!vencimiento && !fecha.atTime(desde).atZone(ZONA).toInstant().isAfter(ahora.toInstant())) {
      throw new IllegalStateException("Solo podés cancelar antes del inicio de la reserva.");
    }
    if (vencimiento && (estado != EstadoReserva.PENDIENTE_PAGO || venceEn.isAfter(ahora))) {
      throw new IllegalStateException("La reserva no está vencida.");
    }
    if (estado == EstadoReserva.CONFIRMADA) {
      saldoTicket = total;
    }
    estado = vencimiento ? EstadoReserva.VENCIDA : EstadoReserva.CANCELADA;
  }

  public void devolverCredito(BigDecimal monto) {
    saldoTicket = saldoTicket.add(monto);
  }

  public BigDecimal importeAPagar() {
    return total.subtract(creditoAplicado);
  }

  public String estadoVisible(OffsetDateTime ahora) {
    return estado == EstadoReserva.CONFIRMADA
            && !fecha.atTime(hasta).atZone(ZONA).toInstant().isAfter(ahora.toInstant())
        ? "FINALIZADA"
        : estado.name();
  }

  public UUID getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Espacio getEspacio() {
    return espacio;
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

  public int getPersonas() {
    return personas;
  }

  public BigDecimal getTarifaHora() {
    return tarifaHora;
  }

  public String getRelacionAplicada() {
    return relacionAplicada;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public EstadoReserva getEstado() {
    return estado;
  }

  public String getCodigo() {
    return codigo;
  }

  public OffsetDateTime getVenceEn() {
    return venceEn;
  }

  public OffsetDateTime getCreadaEn() {
    return creadaEn;
  }

  public UUID getTicketOrigenId() {
    return ticketOrigenId;
  }

  public BigDecimal getCreditoAplicado() {
    return creditoAplicado;
  }

  public BigDecimal getSaldoTicket() {
    return saldoTicket;
  }

  public UUID getClaveSolicitud() {
    return claveSolicitud;
  }
}
