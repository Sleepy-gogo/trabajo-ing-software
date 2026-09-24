package edu.unse.sera.pagos.control;

import edu.unse.sera.membresia.control.MembresiaNoEncontradaException;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.usuario.control.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class PagoService {

  private PagoRepository pagoRepository;
  private MembresiaRepository membresiaRepository;
  private UsuarioRepository usuarioRepository;

  public PagoService(PagoRepository pagoRepository, MembresiaRepository membresiaRepository, UsuarioRepository usuarioRepository){
    this.pagoRepository = pagoRepository;
    this.membresiaRepository = membresiaRepository;
    this.usuarioRepository = usuarioRepository;
  }

  private Pago buscar(UUID id) {
    return pagoRepository.findById(id).orElseThrow(() -> new PagoNoEncontradoException(id));
  }

/*  public PagoDetalle iniciarPagoReserva(UUID idReserva, UUID idUsuario, MedioPago medioPago) {
    if (usuarioRepository.findById(idUsuario).isEmpty()) {
      throw new UsuarioNoEncontradoException(idUsuario);
    }
    // agregar reserva cuando se cree

    //Pago pago = new Pago(ConceptoPago.CUOTA_MENSUAL,usuario, EstadoPago.PENDIENTE,medioPago,monto,membresia);

  }
*/

  public PagoDetalle iniciarPagoCuota(UUID idMembresia, UUID idUsuario, MedioPago medioPago) {

    if (membresiaRepository.findById(idMembresia).isEmpty()) {
      throw new MembresiaNoEncontradaException();
    }

    if (usuarioRepository.findById(idUsuario).isEmpty()) {
      throw new UsuarioNoEncontradoException(idUsuario);
    }
    Membresia membresia = membresiaRepository.getReferenceById(idMembresia);
    Socio membresiaSocio = membresia.getSocio();
    Usuario membresiaUsuario = membresiaSocio.getUsuario();

    if (!membresiaUsuario.getId().equals(idUsuario)) {
      throw new PagoDiscrepanciaException();
    }
    BigDecimal monto = membresia.getNivelMembresia().getPreciosPorRelacion().get(membresiaSocio.getRelacionUnse());
    Pago pago = new Pago(ConceptoPago.CUOTA_MENSUAL,membresiaUsuario, EstadoPago.PENDIENTE,medioPago,monto,membresia);
    return toResponse(pago);
  }

  public PagoDetalle confirmarPago(UUID id, String comprobante) {
    Pago pago = buscar(id);

    pago.aprobar(comprobante);

    return toResponse(pago);
  }




  public PagoDetalle toResponse(Pago pago) {
    return new PagoDetalle(pago.getId(),pago.getConcepto(),pago.getUsuario().getId(),pago.getEstado(),pago.getMedioPago(),pago.getMonto(),pago.getComprobante(),pago.getMembresia().getId());
  }

}
