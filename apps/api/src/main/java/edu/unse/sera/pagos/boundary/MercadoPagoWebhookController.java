package edu.unse.sera.pagos.boundary;

import edu.unse.sera.pagos.control.MercadoPagoGateway;
import edu.unse.sera.pagos.control.SuscripcionMercadoPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class MercadoPagoWebhookController {
  private final edu.unse.sera.reserva.control.ReservaService reservas;
  private final MercadoPagoGateway mercadoPago;
  private final SuscripcionMercadoPagoService suscripciones;

  public MercadoPagoWebhookController(
      MercadoPagoGateway mercadoPago,
      SuscripcionMercadoPagoService suscripciones,
      edu.unse.sera.reserva.control.ReservaService reservas) {
    this.reservas = reservas;
    this.mercadoPago = mercadoPago;
    this.suscripciones = suscripciones;
  }

  @PostMapping
  public ResponseEntity<Void> recibir(
      @RequestHeader(value = "x-signature", required = false) String signature,
      @RequestHeader(value = "x-request-id", required = false) String requestId,
      @RequestParam("data.id") String dataId,
      @RequestParam("type") String type,
      @RequestBody JsonNode body) {
    if (!mercadoPago.firmaValida(signature, requestId, dataId)) {
      return ResponseEntity.status(401).build();
    }
    if (!dataId.equals(body.path("data").path("id").asText())
        || !type.equals(body.path("type").asText())) {
      return ResponseEntity.badRequest().build();
    }
    switch (type) {
      case "subscription_preapproval" -> suscripciones.recibirSuscripcion(dataId);
      case "subscription_authorized_payment" -> {
        try {
          suscripciones.recibirFactura(Long.parseLong(dataId));
        } catch (NumberFormatException e) {
          return ResponseEntity.badRequest().build();
        }
      }
      case "payment" -> {
        try {
          reservas.recibirPago(Long.parseLong(dataId));
        } catch (NumberFormatException e) {
          return ResponseEntity.badRequest().build();
        }
      }
      default -> {
        // Otros tópicos no modifican pagos locales.
      }
    }
    return ResponseEntity.ok().build();
  }
}
