package edu.unse.sera.pagos.control;

import com.mercadopago.resources.preapproval.Preapproval;
import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.entity.SuscripcionMercadoPago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional
public class SuscripcionMercadoPagoService {
  private final SuscripcionMercadoPagoRepository suscripciones;
  private final MembresiaRepository membresias;
  private final PagoRepository pagos;
  private final PagoService pagoService;
  private final MercadoPagoGateway mercadoPago;
  private final TransactionTemplate transacciones;

  public SuscripcionMercadoPagoService(
      SuscripcionMercadoPagoRepository suscripciones,
      MembresiaRepository membresias,
      PagoRepository pagos,
      PagoService pagoService,
      MercadoPagoGateway mercadoPago,
      TransactionTemplate transacciones) {
    this.suscripciones = suscripciones;
    this.membresias = membresias;
    this.pagos = pagos;
    this.pagoService = pagoService;
    this.mercadoPago = mercadoPago;
    this.transacciones = transacciones;
  }

  private record Inicio(
      UUID id,
      String email,
      String nivel,
      BigDecimal monto,
      SuscripcionMercadoPagoDetalle existente) {}

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public SuscripcionMercadoPagoDetalle iniciar(UUID membresiaId, UUID actor) {
    mercadoPago.validarConfiguracion();
    Inicio inicio =
        Objects.requireNonNull(transacciones.execute(status -> reservarInicio(membresiaId, actor)));
    if (inicio.existente() != null) {
      return inicio.existente();
    }
    Preapproval remota;
    try {
      remota = mercadoPago.crear(inicio.id(), inicio.email(), inicio.nivel(), inicio.monto());
    } catch (MercadoPagoSolicitudRechazadaException e) {
      transacciones.execute(
          status -> {
            var rechazada = suscripciones.findById(inicio.id()).orElseThrow();
            if (rechazada.getPreapprovalId() == null) {
              rechazada.actualizarEstado("rejected");
            }
            return null;
          });
      throw e;
    }
    return Objects.requireNonNull(
        transacciones.execute(
            status -> {
              var suscripcion = suscripciones.findById(inicio.id()).orElseThrow();
              validarSuscripcion(suscripcion, remota);
              suscripcion.vincular(remota.getId(), remota.getInitPoint());
              return detalle(suscripcion);
            }));
  }

  private Inicio reservarInicio(UUID membresiaId, UUID actor) {
    Membresia membresia =
        membresias
            .bloquearPorId(membresiaId)
            .orElseThrow(() -> new IllegalArgumentException("Membresía inexistente."));
    if (!membresia.getSocio().getUsuario().getId().equals(actor)) {
      throw new OperacionNoPermitidaException();
    }
    if (membresia.getEstado() != EstadoMembresia.PENDIENTE_PAGO) {
      throw new IllegalStateException("La membresía no espera la autorización de un pago.");
    }
    Pago pagoInicial =
        pagos.findAllByMembresiaId(membresiaId).stream()
            .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
            .filter(p -> p.getMedioPago() == MedioPago.MERCADO_PAGO)
            .filter(p -> membresia.getContratacionId().equals(p.getContratacionId()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No hay un pago de Mercado Pago pendiente."));
    var existente = suscripciones.findByPagoInicialId(pagoInicial.getId());
    if (existente.isPresent()) {
      if ("canceled".equals(existente.get().getEstado())) {
        throw new IllegalStateException("La suscripción fue cancelada. Contratá otra membresía.");
      }
      if (existente.get().getPreapprovalId() == null
          && "rejected".equals(existente.get().getEstado())) {
        existente.get().actualizarEstado("pending");
        return new Inicio(
            existente.get().getId(),
            membresia.getSocio().getUsuario().getEmail(),
            membresia.getNivelMembresia().getNombre(),
            pagoInicial.getMonto(),
            null);
      }
      if (existente.get().getPreapprovalId() == null) {
        throw new IllegalStateException(
            "La solicitud a Mercado Pago tiene un resultado incierto. Consultá a administración.");
      }
      return new Inicio(null, null, null, null, detalle(existente.get()));
    }
    var suscripcion =
        suscripciones.saveAndFlush(new SuscripcionMercadoPago(membresia, pagoInicial));
    return new Inicio(
        suscripcion.getId(),
        membresia.getSocio().getUsuario().getEmail(),
        membresia.getNivelMembresia().getNombre(),
        pagoInicial.getMonto(),
        null);
  }

  @Transactional(readOnly = true)
  public SuscripcionMercadoPagoDetalle consultar(UUID membresiaId, UUID actor) {
    Membresia membresia =
        membresias
            .findById(membresiaId)
            .orElseThrow(() -> new IllegalArgumentException("Membresía inexistente."));
    if (!membresia.getSocio().getUsuario().getId().equals(actor)) {
      throw new OperacionNoPermitidaException();
    }
    return suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId).stream()
        .findFirst()
        .map(this::detalle)
        .orElseThrow(() -> new IllegalArgumentException("La membresía no tiene suscripción."));
  }

  public void cancelarVigente(UUID membresiaId) {
    for (var suscripcion : suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId)) {
      if (suscripcion.getPreapprovalId() == null && "rejected".equals(suscripcion.getEstado())) {
        suscripcion.actualizarEstado("canceled");
      }
      if (suscripcion.getPreapprovalId() == null && !"canceled".equals(suscripcion.getEstado())) {
        throw new IllegalStateException(
            "La suscripción tiene un resultado incierto. Revisala en Mercado Pago antes de cancelar.");
      }
      if (suscripcion.getPreapprovalId() != null && !suscripcion.getEstado().equals("canceled")) {
        Preapproval remota = mercadoPago.obtenerSuscripcion(suscripcion.getPreapprovalId());
        validarSuscripcion(suscripcion, remota);
        if (!"canceled".equals(remota.getStatus()) && !"cancelled".equals(remota.getStatus())) {
          mercadoPago.cancelarSuscripcion(suscripcion.getPreapprovalId());
        }
        suscripcion.actualizarEstado("canceled");
      }
    }
  }

  @Transactional(readOnly = true)
  public void validarSinSuscripcionVigente(UUID membresiaId) {
    boolean vigente =
        suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId).stream()
            .anyMatch(s -> !"canceled".equals(s.getEstado()));
    if (vigente) {
      throw new IllegalStateException(
          "Cancelá la suscripción de Mercado Pago antes de cambiar el nivel.");
    }
  }

  public void recibirSuscripcion(String preapprovalId) {
    var suscripcion = suscripciones.findByPreapprovalId(preapprovalId).orElse(null);
    if (suscripcion == null) {
      Preapproval remota = mercadoPago.obtenerSuscripcion(preapprovalId);
      if (remota == null || !preapprovalId.equals(remota.getId())) {
        throw new MercadoPagoNoDisponibleException();
      }
      UUID referencia = referenciaSera(remota.getExternalReference());
      if (referencia == null) {
        return;
      }
      suscripcion = suscripciones.findById(referencia).orElse(null);
      if (suscripcion == null) {
        throw new MercadoPagoNoDisponibleException();
      }
      validarSuscripcion(suscripcion, remota);
      suscripcion.vincular(preapprovalId, remota.getInitPoint());
      suscripcion.actualizarEstado(remota.getStatus());
      return;
    }
    Preapproval remota = mercadoPago.obtenerSuscripcion(preapprovalId);
    validarSuscripcion(suscripcion, remota);
    suscripcion.actualizarEstado(remota.getStatus());
  }

  public void recibirFactura(long facturaId) {
    FacturaMercadoPago factura = mercadoPago.obtenerFactura(facturaId);
    if (factura.id() != facturaId || factura.paymentId() <= 0) {
      return;
    }
    var suscripcion = suscripciones.findByPreapprovalId(factura.preapprovalId()).orElse(null);
    if (suscripcion == null) {
      recibirSuscripcion(factura.preapprovalId());
      suscripcion = suscripciones.findByPreapprovalId(factura.preapprovalId()).orElse(null);
      if (suscripcion == null) {
        return;
      }
    }
    if (!suscripcion.getId().toString().equals(factura.referencia())
        || !"ARS".equals(factura.moneda())
        || suscripcion.getMonto().compareTo(factura.monto()) != 0) {
      throw new IllegalStateException("La factura no coincide con la suscripción local.");
    }
    var remoto = mercadoPago.obtenerPago(factura.paymentId());
    if (remoto.getId() == null
        || remoto.getId() != factura.paymentId()
        || !"ARS".equals(remoto.getCurrencyId())
        || remoto.getTransactionAmount() == null
        || remoto.getTransactionAmount().compareTo(suscripcion.getMonto()) != 0) {
      throw new IllegalStateException("El cobro no coincide con la factura.");
    }
    if (!"approved".equals(remoto.getStatus())) {
      return;
    }
    if (remoto.getDateApproved() == null) {
      throw new IllegalStateException("Mercado Pago no informó la fecha de aprobación.");
    }
    Preapproval preapproval = mercadoPago.obtenerSuscripcion(factura.preapprovalId());
    validarSuscripcion(suscripcion, preapproval);
    if (remoto.getCollectorId() == null
        || !remoto.getCollectorId().equals(preapproval.getCollectorId())) {
      throw new IllegalStateException("El cobrador no coincide con la suscripción.");
    }
    String paymentId = Long.toString(factura.paymentId());
    if (pagos.findByMercadoPagoPaymentId(paymentId).isPresent()) {
      return;
    }
    boolean facturaYaAplicada =
        pagos.findByMercadoPagoFacturaIdAndEstado(factura.id(), EstadoPago.APROBADO).isPresent();
    Pago pago = suscripcion.getPagoInicial();
    if (pago.getEstado() != EstadoPago.PENDIENTE || facturaYaAplicada) {
      pago =
          pagos.saveAndFlush(
              new Pago(
                  ConceptoPago.CUOTA_MENSUAL,
                  pago.getUsuario(),
                  MedioPago.MERCADO_PAGO,
                  suscripcion.getMonto(),
                  suscripcion.getMembresia()));
    }
    pago.vincularMercadoPagoPaymentId(paymentId);
    pago.vincularFacturaMercadoPago(factura.id());
    if (facturaYaAplicada) {
      pago.aprobar(paymentId, remoto.getDateApproved());
      pago.marcarParaRevision("Otro cobro de la misma factura ya renovó la membresía.");
      return;
    }
    pagoService.confirmarPago(pago.getId(), paymentId, remoto.getDateApproved());
  }

  private void validarSuscripcion(SuscripcionMercadoPago local, Preapproval remota) {
    if (remota == null
        || (local.getPreapprovalId() != null && !local.getPreapprovalId().equals(remota.getId()))
        || !local.getId().toString().equals(remota.getExternalReference())
        || remota.getAutoRecurring() == null
        || !"ARS".equals(remota.getAutoRecurring().getCurrencyId())
        || !Integer.valueOf(1).equals(remota.getAutoRecurring().getFrequency())
        || !"months".equals(remota.getAutoRecurring().getFrequencyType())
        || remota.getAutoRecurring().getTransactionAmount() == null
        || local.getMonto().compareTo(remota.getAutoRecurring().getTransactionAmount()) != 0) {
      throw new IllegalStateException("La suscripción remota no coincide con la local.");
    }
  }

  private SuscripcionMercadoPagoDetalle detalle(SuscripcionMercadoPago suscripcion) {
    return new SuscripcionMercadoPagoDetalle(
        suscripcion.getId(),
        suscripcion.getPreapprovalId(),
        suscripcion.getEstado(),
        suscripcion.getCheckoutUrl());
  }

  private UUID referenciaSera(String referencia) {
    if (referencia == null) {
      return null;
    }
    try {
      return UUID.fromString(referencia);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
