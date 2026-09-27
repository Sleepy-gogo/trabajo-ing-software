package edu.unse.sera.pagos.boundary;

import edu.unse.sera.pagos.boundary.dto.SuscripcionResponse;
import edu.unse.sera.pagos.control.SuscripcionMercadoPagoService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

  private final SuscripcionMercadoPagoService suscripciones;

  public PagoController(SuscripcionMercadoPagoService suscripciones) {
    this.suscripciones = suscripciones;
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
