package edu.unse.sera.reserva.control;

import edu.unse.sera.disponibilidad.control.CalendarioService;
import edu.unse.sera.espacio.control.EspacioNoEncontradoException;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.pagos.control.MercadoPagoGateway;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.reserva.entity.EstadoReserva;
import edu.unse.sera.reserva.entity.Reserva;
import edu.unse.sera.reserva.persistence.ReservaRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.usuario.control.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional
public class ReservaService {
  private final ReservaRepository reservas;
  private final UsuarioRepository usuarios;
  private final EspacioRepository espacios;
  private final CalendarioService calendario;
  private final PagoRepository pagos;
  private final MercadoPagoGateway mercadoPago;
  private final TransactionTemplate transacciones;

  public ReservaService(
      ReservaRepository reservas,
      UsuarioRepository usuarios,
      EspacioRepository espacios,
      CalendarioService calendario,
      PagoRepository pagos,
      MercadoPagoGateway mercadoPago,
      TransactionTemplate transacciones) {
    this.reservas = reservas;
    this.usuarios = usuarios;
    this.espacios = espacios;
    this.calendario = calendario;
    this.pagos = pagos;
    this.mercadoPago = mercadoPago;
    this.transacciones = transacciones;
  }

  public record Solicitud(
      UUID espacioId,
      LocalDate fecha,
      LocalTime desde,
      LocalTime hasta,
      int personas,
      UUID ticketId,
      MedioPago medioPago,
      UUID claveSolicitud) {}

  public record Precio(
      BigDecimal tarifaHora,
      String relacionAplicada,
      BigDecimal total,
      BigDecimal creditoAplicado,
      BigDecimal aPagar) {}

  @Transactional(readOnly = true)
  public Precio cotizar(UUID actor, Solicitud solicitud) {
    Reserva reserva = preparar(usuario(actor), solicitud);
    BigDecimal credito = BigDecimal.ZERO;
    if (solicitud.ticketId() != null) {
      Reserva ticket = ticket(solicitud.ticketId(), actor);
      credito = ticket.getSaldoTicket().min(reserva.getTotal());
    }
    return new Precio(
        reserva.getTarifaHora(),
        reserva.getRelacionAplicada(),
        reserva.getTotal(),
        credito,
        reserva.getTotal().subtract(credito));
  }

  public ReservaDetalle crear(UUID actor, Solicitud solicitud) {
    if (solicitud.claveSolicitud() == null || solicitud.medioPago() == null) {
      throw new IllegalArgumentException("Indicá el medio de pago y la clave de solicitud.");
    }
    // El mismo orden de locks protege tanto el horario como el saldo de tickets.
    Usuario usuario = bloquearUsuario(actor);
    var existente = reservas.findByUsuarioIdAndClaveSolicitud(actor, solicitud.claveSolicitud());
    if (existente.isPresent()) {
      Reserva r = existente.get();
      if (!r.getEspacio().getId().equals(solicitud.espacioId())
          || !r.getFecha().equals(solicitud.fecha())
          || !r.getDesde().equals(solicitud.desde())
          || !r.getHasta().equals(solicitud.hasta())
          || r.getPersonas() != solicitud.personas()
          || !Objects.equals(r.getTicketOrigenId(), solicitud.ticketId())
          || pagos
              .findByReservaId(r.getId())
              .map(p -> p.getMedioPago() != solicitud.medioPago())
              .orElse(false)) {
        throw new IllegalStateException("La solicitud ya pertenece a otra reserva.");
      }
      return detalle(r);
    }
    espacios
        .bloquearPorId(solicitud.espacioId())
        .orElseThrow(() -> new EspacioNoEncontradoException(solicitud.espacioId()));
    Reserva reserva = preparar(usuario, solicitud);
    if (solicitud.ticketId() != null) {
      reserva.aplicarTicket(ticket(solicitud.ticketId(), actor));
    }
    reservas.saveAndFlush(reserva);
    if (reserva.importeAPagar().signum() == 0) {
      reserva.confirmar(ahora());
    } else {
      pagos.saveAndFlush(new Pago(reserva, solicitud.medioPago()));
    }
    return detalle(reserva);
  }

  private Reserva preparar(Usuario usuario, Solicitud s) {
    if (usuario.getEstadoCuenta() != EstadoUsuario.ACTIVO) {
      throw new OperacionNoPermitidaException();
    }
    if (s.espacioId() == null) {
      throw new IllegalArgumentException("Elegí un espacio.");
    }
    Reserva.validarHorario(s.fecha(), s.desde(), s.hasta(), ahora());
    var espacio =
        espacios
            .findById(s.espacioId())
            .orElseThrow(() -> new EspacioNoEncontradoException(s.espacioId()));
    var disponible = calendario.consultar(s.espacioId(), s.fecha(), usuario.getId());
    if (disponible.franjas().stream()
        .noneMatch(f -> !s.desde().isBefore(f.desde()) && !s.hasta().isAfter(f.hasta()))) {
      throw new IllegalStateException("El horario ya no está disponible. Elegí otra franja.");
    }
    return new Reserva(
        usuario,
        espacio,
        s.fecha(),
        s.desde(),
        s.hasta(),
        s.personas(),
        disponible.tarifaHora(),
        disponible.relacionAplicada().name(),
        s.claveSolicitud(),
        ahora());
  }

  @Transactional(readOnly = true)
  public List<ReservaDetalle> listar(UUID actor, boolean todas) {
    if (todas && usuario(actor).getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
    return reservas.listar(todas ? null : actor).stream().map(this::detalle).toList();
  }

  @Transactional(readOnly = true)
  public ReservaDetalle consultar(UUID id, UUID actor) {
    Reserva reserva = buscar(id);
    autorizar(reserva, actor);
    return detalle(reserva);
  }

  public ReservaDetalle cancelar(UUID id, UUID actor) {
    Reserva reserva = bloquear(id);
    autorizar(reserva, actor);
    cancelarInterno(reserva, false);
    return detalle(reserva);
  }

  private void cancelarInterno(Reserva reserva, boolean vencimiento) {
    EstadoReserva anterior = reserva.getEstado();
    if (anterior == EstadoReserva.CANCELADA || anterior == EstadoReserva.VENCIDA) {
      return;
    }
    reserva.cancelar(ahora(), vencimiento);
    if (anterior == EstadoReserva.PENDIENTE_PAGO) {
      if (reserva.getTicketOrigenId() != null) {
        buscar(reserva.getTicketOrigenId()).devolverCredito(reserva.getCreditoAplicado());
      }
      pagos
          .findByReservaId(reserva.getId())
          .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
          .ifPresent(Pago::cancelarPendiente);
    }
  }

  public void confirmarPago(UUID pagoId, String comprobante, OffsetDateTime fechaPago) {
    UUID reservaId = pagos.findById(pagoId).orElseThrow().getReservaId();
    Reserva reserva = bloquear(reservaId);
    Pago pago = pagos.findById(pagoId).orElseThrow();
    if (!pago.aprobar(comprobante, fechaPago)) {
      return;
    }
    if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO
        || !reserva.getVenceEn().isAfter(ahora())) {
      pago.marcarParaRevision(
          "La reserva fue cancelada o venció. Revisar el cobro sin recuperar el horario.");
      return;
    }
    reserva.confirmar(ahora());
  }

  public ReservaDetalle checkout(UUID id, UUID actor) {
    Reserva reserva = bloquear(id);
    autorizar(reserva, actor);
    if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO
        || !reserva.getVenceEn().isAfter(ahora())) {
      throw new IllegalStateException("La reserva ya no está pendiente de pago.");
    }
    Pago pago = pagos.findByReservaId(id).orElseThrow();
    if (pago.getMedioPago() != MedioPago.MERCADO_PAGO || pago.getEstado() != EstadoPago.PENDIENTE) {
      throw new IllegalStateException("La reserva no tiene un pago pendiente de Mercado Pago.");
    }
    if (pago.getCheckoutUrl() == null) {
      pago.vincularCheckout(
          mercadoPago.crearCheckout(
              pago.getId(),
              id,
              reserva.getEspacio().getNombre(),
              pago.getMonto(),
              reserva.getVenceEn()));
    }
    return detalle(reserva);
  }

  public void recibirPago(long idRemoto) {
    var remoto = mercadoPago.obtenerPago(idRemoto);
    UUID referencia;
    try {
      referencia = UUID.fromString(remoto.getExternalReference());
    } catch (IllegalArgumentException | NullPointerException e) {
      return;
    }
    var local = pagos.findById(referencia);
    if (local.isEmpty() || local.get().getReservaId() == null) {
      return;
    }
    Reserva reserva = bloquear(local.get().getReservaId());
    Pago pago = pagos.findById(referencia).orElseThrow();
    if (pago.getMedioPago() != MedioPago.MERCADO_PAGO
        || !Long.valueOf(idRemoto).equals(remoto.getId())
        || !"ARS".equals(remoto.getCurrencyId())
        || remoto.getTransactionAmount() == null
        || pago.getMonto().compareTo(remoto.getTransactionAmount()) != 0) {
      throw new IllegalStateException("El cobro de Mercado Pago no coincide con la reserva.");
    }
    if (!"approved".equals(remoto.getStatus())) {
      return;
    }
    if (remoto.getDateApproved() == null) {
      throw new IllegalStateException("El cobro no tiene fecha de aprobación.");
    }
    pago.vincularMercadoPagoPaymentId(Long.toString(idRemoto));
    confirmarPago(pago.getId(), "MP-" + idRemoto, remoto.getDateApproved());
  }

  public ReservaDetalle verificarPago(UUID id, UUID actor, long idRemoto) {
    Reserva reserva = buscar(id);
    autorizar(reserva, actor);
    var remoto = mercadoPago.obtenerPago(idRemoto);
    Pago pago = pagos.findByReservaId(id).orElseThrow();
    if (!pago.getId().toString().equals(remoto.getExternalReference())) {
      throw new IllegalArgumentException("El pago no corresponde a esta reserva.");
    }
    recibirPago(idRemoto);
    return detalle(reserva);
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public void vencerPendientes() {
    for (UUID id : reservas.vencidas(ahora())) {
      transacciones.executeWithoutResult(
          status -> {
            Reserva r = bloquear(id);
            if (r.getEstado() == EstadoReserva.PENDIENTE_PAGO && !r.getVenceEn().isAfter(ahora())) {
              cancelarInterno(r, true);
            }
          });
    }
  }

  private Reserva ticket(UUID id, UUID actor) {
    Reserva ticket = buscar(id);
    if (!ticket.getUsuario().getId().equals(actor)) {
      throw new OperacionNoPermitidaException();
    }
    if (ticket.getEstado() != EstadoReserva.CANCELADA || ticket.getSaldoTicket().signum() <= 0) {
      throw new IllegalStateException("El ticket ya no tiene saldo disponible.");
    }
    return ticket;
  }

  private Reserva bloquear(UUID id) {
    // Consultar solo el titular antes de tomar el lock evita cargar un estado desactualizado.
    UUID titular = reservas.titular(id).orElseThrow(ReservaNoEncontradaException::new);
    bloquearUsuario(titular);
    Reserva reserva = buscar(id);
    espacios.bloquearPorId(reserva.getEspacio().getId()).orElseThrow();
    return reserva;
  }

  private Usuario bloquearUsuario(UUID id) {
    return usuarios.bloquearPorId(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
  }

  private Usuario usuario(UUID id) {
    return usuarios.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
  }

  private Reserva buscar(UUID id) {
    return reservas.findById(id).orElseThrow(ReservaNoEncontradaException::new);
  }

  private void autorizar(Reserva r, UUID actor) {
    if (!r.getUsuario().getId().equals(actor) && usuario(actor).getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
  }

  private OffsetDateTime ahora() {
    return OffsetDateTime.now(Reserva.ZONA);
  }

  private ReservaDetalle detalle(Reserva r) {
    Pago pago = pagos.findByReservaId(r.getId()).orElse(null);
    return new ReservaDetalle(
        r.getId(),
        r.getUsuario().getId(),
        r.getUsuario().getNombreCompleto(),
        r.getEspacio().getId(),
        r.getEspacio().getNombre(),
        r.getFecha(),
        r.getDesde(),
        r.getHasta(),
        r.getPersonas(),
        r.getTarifaHora(),
        r.getRelacionAplicada(),
        r.getTotal(),
        r.getCreditoAplicado(),
        r.getSaldoTicket(),
        r.estadoVisible(ahora()),
        r.getCodigo(),
        r.getVenceEn(),
        pago == null ? null : pago.getId(),
        pago == null ? null : pago.getEstado(),
        pago == null ? null : pago.getMedioPago(),
        pago == null ? null : pago.getCheckoutUrl(),
        pago != null && pago.isRequiereRevision(),
        (r.getEstado() == EstadoReserva.PENDIENTE_PAGO || r.getEstado() == EstadoReserva.CONFIRMADA)
            && r.getFecha()
                .atTime(r.getDesde())
                .atZone(Reserva.ZONA)
                .toInstant()
                .isAfter(ahora().toInstant()));
  }
}
