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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SuscripcionMercadoPagoService {
  private final SuscripcionMercadoPagoRepository suscripciones;
  private final MembresiaRepository membresias;
  private final PagoRepository pagos;
  private final PagoService pagoService;
  private final MercadoPagoGateway mercadoPago;

  public SuscripcionMercadoPagoService(
      SuscripcionMercadoPagoRepository suscripciones,
      MembresiaRepository membresias,
      PagoRepository pagos,
      PagoService pagoService,
      MercadoPagoGateway mercadoPago) {
    this.suscripciones = suscripciones;
    this.membresias = membresias;
    this.pagos = pagos;
    this.pagoService = pagoService;
    this.mercadoPago = mercadoPago;
  }

  public SuscripcionMercadoPagoDetalle iniciar(UUID membresiaId, UUID actor) {
    Membresia membresia =
        membresias
            .findById(membresiaId)
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
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No hay un pago de Mercado Pago pendiente."));
    var existente = suscripciones.findByPagoInicialId(pagoInicial.getId());
    if (existente.isPresent()) {
      if ("canceled".equals(existente.get().getEstado())) {
        throw new IllegalStateException("La suscripción fue cancelada. Contratá otra membresía.");
      }
      return detalle(existente.get());
    }
    var suscripcion =
        suscripciones.saveAndFlush(new SuscripcionMercadoPago(membresia, pagoInicial));
    Preapproval remota =
        mercadoPago.crear(
            suscripcion.getId(),
            membresia.getSocio().getUsuario().getEmail(),
            membresia.getNivelMembresia().getNombre(),
            pagoInicial.getMonto());
    validarSuscripcion(suscripcion, remota);
    suscripcion.vincular(remota.getId(), remota.getInitPoint());
    return detalle(suscripcion);
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
      if (suscripcion.getPreapprovalId() != null && !suscripcion.getEstado().equals("canceled")) {
        Preapproval remota = mercadoPago.obtenerSuscripcion(suscripcion.getPreapprovalId());
        validarSuscripcion(suscripcion, remota);
        if (!"canceled".equals(remota.getStatus())) {
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
      if (referenciaSera(remota.getExternalReference())) {
        throw new MercadoPagoNoDisponibleException();
      }
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
      if (referenciaSera(factura.referencia())) {
        throw new MercadoPagoNoDisponibleException();
      }
      return;
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

  private boolean referenciaSera(String referencia) {
    if (referencia == null) {
      return false;
    }
    try {
      UUID.fromString(referencia);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
