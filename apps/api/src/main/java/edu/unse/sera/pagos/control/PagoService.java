package edu.unse.sera.pagos.control;

import edu.unse.sera.membresia.control.MembresiaNoEncontradaException;
import edu.unse.sera.membresia.control.MembresiaService;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.usuario.control.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class PagoService {

  private PagoRepository pagoRepository;
  private MembresiaRepository membresiaRepository;
  private UsuarioRepository usuarioRepository;
  private final MembresiaService membresiaService;

  public PagoService(
      PagoRepository pagoRepository,
      MembresiaRepository membresiaRepository,
      UsuarioRepository usuarioRepository,
      MembresiaService membresiaService) {
    this.pagoRepository = pagoRepository;
    this.membresiaRepository = membresiaRepository;
    this.usuarioRepository = usuarioRepository;
    this.membresiaService = membresiaService;
  }

  private Pago buscar(UUID id) {
    return pagoRepository.findById(id).orElseThrow(() -> new PagoNoEncontradoException(id));
  }

  public PagoDetalle iniciarPagoCuota(UUID idMembresia, UUID idUsuario, MedioPago medioPago) {

    if (membresiaRepository.findById(idMembresia).isEmpty()) {
      throw new MembresiaNoEncontradaException();
    }

    if (usuarioRepository.findById(idUsuario).isEmpty()) {
      throw new UsuarioNoEncontradoException(idUsuario);
    }
    Membresia membresia = membresiaRepository.getReferenceById(idMembresia);
    if (!membresia.admitePago()) {
      throw new IllegalStateException("La membresía no admite pagos en su estado actual.");
    }
    Socio membresiaSocio = membresia.getSocio();
    Usuario membresiaUsuario = membresiaSocio.getUsuario();

    if (!membresiaUsuario.getId().equals(idUsuario)) {
      throw new PagoDiscrepanciaException();
    }
    if (pagoRepository.existsByMembresiaIdAndEstado(idMembresia, EstadoPago.PENDIENTE)) {
      throw new IllegalStateException("Ya existe un pago pendiente para esta membresía.");
    }
    RelacionUnse tarifaRelacion =
        membresiaSocio.getEstadoVerificacionUnse() == EstadoVerificacionUnse.VERIFICADA
            ? membresiaSocio.getRelacionUnse()
            : RelacionUnse.EXTERNO;
    BigDecimal monto = membresia.getNivelMembresia().getPreciosPorRelacion().get(tarifaRelacion);
    if (monto == null) {
      throw new IllegalArgumentException(
          "El nivel no tiene una tarifa disponible para esta relación.");
    }
    Pago pago = new Pago(ConceptoPago.CUOTA_MENSUAL, membresiaUsuario, medioPago, monto, membresia);
    pagoRepository.save(pago);
    return toResponse(pago);
  }

  public PagoDetalle confirmarPago(UUID id, String comprobante) {
    Pago pago = buscar(id);
    if (pago.getEstado() == EstadoPago.APROBADO) {
      return toResponse(pago);
    }
    switch (pago.getConcepto()) {
      case ConceptoPago.CUOTA_MENSUAL:
        Membresia membresia = pago.getMembresia();
        if (membresia != null && !membresia.admitePago()) {
          throw new IllegalStateException("La membresía ya no admite este pago.");
        }
        pago.aprobar(comprobante);
        if (membresia != null) {
          membresiaService.activarMembresia(pago, OffsetDateTime.now());
        }
        break;
      case ConceptoPago.RESERVA:
      case ConceptoPago.DIFERENCIA_TICKET:
        // TODO: Implementar
        pago.aprobar(comprobante);
        break;
    }
    return toResponse(pago);
  }

  public PagoDetalle rechazarPago(UUID id) {
    Pago pago = buscar(id);
    pago.rechazar();
    return toResponse(pago);
  }

  public PagoDetalle toResponse(Pago pago) {
    Membresia membresia = pago.getMembresia();
    return new PagoDetalle(
        pago.getId(),
        pago.getConcepto(),
        pago.getUsuario().getId(),
        pago.getEstado(),
        pago.getMedioPago(),
        pago.getMonto(),
        pago.getComprobante(),
        membresia == null ? null : membresia.getId());
  }
}
