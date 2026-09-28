package edu.unse.sera.pagos.control;

import edu.unse.sera.membresia.control.MembresiaNoEncontradaException;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.usuario.control.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PagoService {
  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private final PagoRepository pagos;
  private final MembresiaRepository membresias;
  private final UsuarioRepository usuarios;
  private final SuscripcionMercadoPagoRepository suscripciones;

  public PagoService(
      PagoRepository pagos,
      MembresiaRepository membresias,
      UsuarioRepository usuarios,
      SuscripcionMercadoPagoRepository suscripciones) {
    this.pagos = pagos;
    this.membresias = membresias;
    this.usuarios = usuarios;
    this.suscripciones = suscripciones;
  }

  public PagoDetalle iniciarPagoCuota(
      UUID membresiaId, UUID actorId, MedioPago medio, UUID claveSolicitud) {
    if (medio == null || claveSolicitud == null) {
      throw new IllegalArgumentException("Indicá el medio y la clave de solicitud.");
    }
    Membresia membresia =
        membresias.bloquearPorId(membresiaId).orElseThrow(MembresiaNoEncontradaException::new);
    autorizar(membresia.getSocio().getUsuario().getId(), actorId);
    if (membresia.getSocio().getUsuario().getEstadoCuenta() != EstadoUsuario.ACTIVO
        || !membresia.admitePago()) {
      throw new IllegalStateException("La membresía no admite este pago.");
    }
    if (medio == MedioPago.EFECTIVO
        && suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId).stream()
            .anyMatch(s -> !"canceled".equals(s.getEstado()))) {
      throw new IllegalStateException(
          "La membresía tiene un cobro mensual activo en Mercado Pago.");
    }
    var repetido =
        pagos.findByUsuarioIdAndClaveSolicitud(
            membresia.getSocio().getUsuario().getId(), claveSolicitud);
    if (repetido.isPresent()) {
      Pago pago = repetido.get();
      if (!membresiaId.equals(pago.getMembresia().getId()) || medio != pago.getMedioPago()) {
        throw new IllegalStateException("La clave ya pertenece a otra operación.");
      }
      return detalle(pago);
    }
    var pendiente =
        pagos.findByMembresiaIdAndContratacionIdAndEstado(
            membresiaId, membresia.getContratacionId(), EstadoPago.PENDIENTE);
    if (pendiente.isPresent()) {
      if (pendiente.get().getMedioPago() != medio) {
        throw new IllegalStateException("Ya existe un pago pendiente con otro medio.");
      }
      return detalle(pendiente.get());
    }
    var socio = membresia.getSocio();
    BigDecimal monto =
        membresia.getNivelMembresia().getPreciosPorRelacion().get(socio.relacionParaTarifa());
    if (monto == null) {
      throw new IllegalArgumentException("El nivel no tiene una tarifa para esta relación.");
    }
    Pago pago =
        pagos.saveAndFlush(
            new Pago(
                ConceptoPago.CUOTA_MENSUAL,
                socio.getUsuario(),
                medio,
                monto,
                membresia,
                claveSolicitud));
    return detalle(pago);
  }

  public PagoDetalle confirmarPagoEfectivo(UUID pagoId, UUID adminId) {
    Usuario admin = usuario(adminId);
    if (admin.getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
    Pago pago = buscar(pagoId);
    if (pago.getMedioPago() != MedioPago.EFECTIVO) {
      throw new IllegalStateException("Solo se confirma efectivo desde administración.");
    }
    if (pago.getEstado() != EstadoPago.PENDIENTE && pago.getEstado() != EstadoPago.APROBADO) {
      throw new IllegalStateException("Este pago ya no está pendiente.");
    }
    return confirmarPago(pagoId, "SERA-" + pagoId, OffsetDateTime.now(ZoneOffset.UTC));
  }

  /** Solo la integración verificada con el proveedor llama este método para Mercado Pago. */
  public PagoDetalle confirmarPago(UUID pagoId, String comprobante, OffsetDateTime fechaPago) {
    Pago pago = buscar(pagoId);
    if (pago.getMembresia() == null) {
      throw new IllegalStateException("El pago no tiene una membresía.");
    }
    Membresia membresia =
        membresias
            .bloquearPorId(pago.getMembresia().getId())
            .orElseThrow(MembresiaNoEncontradaException::new);
    boolean nuevo = pago.aprobar(comprobante, fechaPago);
    if (!nuevo) {
      return detalle(pago);
    }
    if (!membresia.getContratacionId().equals(pago.getContratacionId())
        || !membresia.admitePago()
        || !membresia.getSocio().getUsuario().getId().equals(pago.getUsuario().getId())) {
      pago.marcarParaRevision("El cobro pertenece a una contratación que ya no admite pagos.");
      return detalle(pago);
    }
    LocalDate anterior = membresia.getProximoVencimiento();
    LocalDate fechaNegocio = fechaPago.atZoneSameInstant(ZONA).toLocalDate();
    LocalDate nuevoVencimiento = membresia.renovarUnMes(fechaNegocio, LocalDate.now(ZONA));
    pago.marcarAplicado(anterior, nuevoVencimiento, OffsetDateTime.now(ZoneOffset.UTC));
    return detalle(pago);
  }

  public PagoDetalle rechazarPago(UUID id) {
    Pago pago = buscar(id);
    pago.rechazar();
    return detalle(pago);
  }

  public PagoDetalle cancelarPendiente(UUID id, UUID actorId) {
    Pago pago = buscar(id);
    autorizar(pago.getUsuario().getId(), actorId);
    if (pago.getMedioPago() == MedioPago.MERCADO_PAGO) {
      throw new IllegalStateException("Cancelá primero la suscripción de Mercado Pago.");
    }
    pago.cancelarPendiente();
    return detalle(pago);
  }

  @Transactional(readOnly = true)
  public PagoDetalle consultar(UUID id, UUID actorId) {
    Pago pago = buscar(id);
    autorizar(pago.getUsuario().getId(), actorId);
    return detalle(pago);
  }

  @Transactional(readOnly = true)
  public Page<PagoDetalle> listar(UUID actorId, UUID usuarioId, EstadoPago estado, int pagina) {
    Usuario actor = usuario(actorId);
    if (actor.getRol() != RolUsuario.ADMIN) {
      usuarioId = actorId;
    }
    return pagos.buscarHistorial(usuarioId, estado, PageRequest.of(pagina, 20)).map(this::detalle);
  }

  @Transactional(readOnly = true)
  public PagoDetalle consultarComprobante(UUID id, UUID actorId) {
    PagoDetalle pago = consultar(id, actorId);
    if (pago.estado() != EstadoPago.APROBADO) {
      throw new IllegalStateException("El pago todavía no tiene comprobante.");
    }
    return pago;
  }

  private Usuario usuario(UUID id) {
    return usuarios.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
  }

  private void autorizar(UUID titularId, UUID actorId) {
    if (!titularId.equals(actorId) && usuario(actorId).getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
  }

  private Pago buscar(UUID id) {
    return pagos.findById(id).orElseThrow(() -> new PagoNoEncontradoException(id));
  }

  private PagoDetalle detalle(Pago pago) {
    return new PagoDetalle(
        pago.getId(),
        pago.getConcepto(),
        pago.getUsuario().getId(),
        pago.getUsuario().getNombreCompleto(),
        pago.getEstado(),
        pago.getMedioPago(),
        pago.getMonto(),
        pago.getComprobante(),
        pago.getMembresia() == null ? null : pago.getMembresia().getId(),
        pago.getCreatedAt(),
        pago.getAprobadoEn(),
        pago.getAplicadoEn(),
        pago.getVencimientoResultante(),
        pago.isRequiereRevision(),
        pago.getMotivoRevision());
  }
}
