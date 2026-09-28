package edu.unse.sera.pagos.boundary;

import edu.unse.sera.pagos.boundary.dto.SuscripcionResponse;
import edu.unse.sera.pagos.control.PagoDetalle;
import edu.unse.sera.pagos.control.PagoService;
import edu.unse.sera.pagos.control.SuscripcionMercadoPagoService;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

  private final SuscripcionMercadoPagoService suscripciones;
  private final PagoService pagos;

  public PagoController(SuscripcionMercadoPagoService suscripciones, PagoService pagos) {
    this.suscripciones = suscripciones;
    this.pagos = pagos;
  }

  public record IniciarPagoRequest(MedioPago medioPago, UUID claveSolicitud) {}

  @PostMapping("/membresias/{id}")
  public PagoDetalle iniciar(
      @PathVariable UUID id, @RequestBody IniciarPagoRequest request, Principal actor) {
    return pagos.iniciarPagoCuota(
        id, UUID.fromString(actor.getName()), request.medioPago(), request.claveSolicitud());
  }

  @GetMapping
  public Page<PagoDetalle> listar(
      @RequestParam(required = false) UUID usuarioId,
      @RequestParam(required = false) EstadoPago estado,
      @RequestParam(defaultValue = "0") int pagina,
      Principal actor) {
    return pagos.listar(UUID.fromString(actor.getName()), usuarioId, estado, pagina);
  }

  @GetMapping("/{id}")
  public PagoDetalle consultar(@PathVariable UUID id, Principal actor) {
    return pagos.consultar(id, UUID.fromString(actor.getName()));
  }

  @GetMapping("/{id}/comprobante")
  public PagoDetalle comprobante(@PathVariable UUID id, Principal actor) {
    return pagos.consultarComprobante(id, UUID.fromString(actor.getName()));
  }

  @PostMapping("/{id}/confirmacion-efectivo")
  public PagoDetalle confirmarEfectivo(@PathVariable UUID id, Principal actor) {
    return pagos.confirmarPagoEfectivo(id, UUID.fromString(actor.getName()));
  }

  @PostMapping("/{id}/cancelacion")
  public PagoDetalle cancelar(@PathVariable UUID id, Principal actor) {
    return pagos.cancelarPendiente(id, UUID.fromString(actor.getName()));
  }

  @PostMapping("/membresias/{id}/suscripcion")
  public SuscripcionResponse iniciarSuscripcion(@PathVariable UUID id, Principal actor) {
    return SuscripcionResponse.from(suscripciones.iniciar(id, UUID.fromString(actor.getName())));
  }

  @GetMapping("/membresias/{id}/suscripcion")
  public SuscripcionResponse consultarSuscripcion(@PathVariable UUID id, Principal actor) {
    return SuscripcionResponse.from(suscripciones.consultar(id, UUID.fromString(actor.getName())));
  }
}
