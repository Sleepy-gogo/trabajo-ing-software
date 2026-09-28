package edu.unse.sera.pagos.control;

import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Recupera facturas cuando no llegó la notificación de Mercado Pago. */
@Service
public class ConciliacionPagoService {
  private final SuscripcionMercadoPagoRepository suscripciones;
  private final UsuarioRepository usuarios;
  private final MercadoPagoGateway mercadoPago;
  private final SuscripcionMercadoPagoService procesador;

  public ConciliacionPagoService(
      SuscripcionMercadoPagoRepository suscripciones,
      UsuarioRepository usuarios,
      MercadoPagoGateway mercadoPago,
      SuscripcionMercadoPagoService procesador) {
    this.suscripciones = suscripciones;
    this.usuarios = usuarios;
    this.mercadoPago = mercadoPago;
    this.procesador = procesador;
  }

  public int conciliar(UUID membresiaId, UUID actorId) {
    if (usuarios.findById(actorId).filter(u -> u.getRol() == RolUsuario.ADMIN).isEmpty()) {
      throw new OperacionNoPermitidaException();
    }
    if (membresiaId == null) {
      throw new IllegalArgumentException("La membresía es obligatoria.");
    }
    int revisadas = 0;
    for (var suscripcion : suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId)) {
      if (suscripcion.getPreapprovalId() == null) {
        continue;
      }
      int offset = 0;
      int total;
      do {
        var pagina = mercadoPago.buscarFacturas(suscripcion.getPreapprovalId(), offset);
        total = pagina.total();
        if (pagina.ids().isEmpty() && offset < total) {
          throw new MercadoPagoNoDisponibleException();
        }
        for (long facturaId : pagina.ids()) {
          procesador.recibirFactura(facturaId);
          revisadas++;
        }
        offset += pagina.ids().size();
      } while (offset < total);
    }
    return revisadas;
  }
}
