package edu.unse.sera.reserva.boundary;

import edu.unse.sera.reserva.control.ReservaDetalle;
import edu.unse.sera.reserva.control.ReservaService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
  private final ReservaService reservas;

  public ReservaController(ReservaService reservas) {
    this.reservas = reservas;
  }

  @PostMapping("/cotizacion")
  public ReservaService.Precio cotizar(
      @RequestBody ReservaService.Solicitud solicitud, Principal actor) {
    return reservas.cotizar(UUID.fromString(actor.getName()), solicitud);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReservaDetalle crear(@RequestBody ReservaService.Solicitud solicitud, Principal actor) {
    return reservas.crear(UUID.fromString(actor.getName()), solicitud);
  }

  @GetMapping
  public List<ReservaDetalle> listar(
      @RequestParam(defaultValue = "false") boolean todas, Principal actor) {
    return reservas.listar(UUID.fromString(actor.getName()), todas);
  }

  @GetMapping("/{id}")
  public ReservaDetalle consultar(@PathVariable UUID id, Principal actor) {
    return reservas.consultar(id, UUID.fromString(actor.getName()));
  }

  @PostMapping("/{id}/cancelacion")
  public ReservaDetalle cancelar(@PathVariable UUID id, Principal actor) {
    return reservas.cancelar(id, UUID.fromString(actor.getName()));
  }

  @PostMapping("/{id}/checkout")
  public ReservaDetalle checkout(@PathVariable UUID id, Principal actor) {
    return reservas.checkout(id, UUID.fromString(actor.getName()));
  }

  public record VerificarPagoRequest(long pagoId) {}

  @PostMapping("/{id}/verificar-pago")
  public ReservaDetalle verificar(
      @PathVariable UUID id, @RequestBody VerificarPagoRequest request, Principal actor) {
    return reservas.verificarPago(id, UUID.fromString(actor.getName()), request.pagoId());
  }
}
