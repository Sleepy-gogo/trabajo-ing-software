package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MercadoPagoGatewayTest {
  @Test
  void checkoutDeReservaUsaImporteReferenciaYVencimientoLocales() throws Exception {
    var respuesta = org.mockito.Mockito.mock(com.mercadopago.resources.preference.Preference.class);
    org.mockito.Mockito.when(respuesta.getInitPoint())
        .thenReturn("https://www.mercadopago.com.ar/checkout/test");
    try (var clientes =
        org.mockito.Mockito.mockConstruction(
            com.mercadopago.client.preference.PreferenceClient.class,
            (cliente, contexto) ->
                org.mockito.Mockito.when(
                        cliente.create(
                            org.mockito.ArgumentMatchers.any(
                                com.mercadopago.client.preference.PreferenceRequest.class)))
                    .thenReturn(respuesta))) {
      var gateway =
          new MercadoPagoGateway(
              "token-demo", "", "https://sera.example/app/payments", new ObjectMapper());
      var pago = java.util.UUID.randomUUID();
      var reserva = java.util.UUID.randomUUID();
      var vence = java.time.OffsetDateTime.now().plusHours(1);
      gateway.crearCheckout(pago, reserva, "Cancha", new java.math.BigDecimal("2500"), vence);
      var solicitud =
          org.mockito.ArgumentCaptor.forClass(
              com.mercadopago.client.preference.PreferenceRequest.class);
      org.mockito.Mockito.verify(clientes.constructed().getFirst()).create(solicitud.capture());
      assertThat(solicitud.getValue().getExternalReference()).isEqualTo(pago.toString());
      assertThat(solicitud.getValue().getExpirationDateTo()).isEqualTo(vence);
      assertThat(solicitud.getValue().getBackUrls().getSuccess())
          .isEqualTo("https://sera.example/app/reservations/" + reserva);
      assertThat(solicitud.getValue().getItems().getFirst().getUnitPrice())
          .isEqualByComparingTo("2500");
    }
  }

  @Test
  void validaFirmaOficialYRechazaIdentificadorAlterado() throws Exception {
    String manifest = "id:123;request-id:request-1;ts:1704908010;";
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec("secreto".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature =
        "ts=1704908010,v1="
            + HexFormat.of().formatHex(hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    var gateway = new MercadoPagoGateway("", "secreto", "", new ObjectMapper());

    assertThat(gateway.firmaValida(signature, "request-1", "123")).isTrue();
    assertThat(gateway.firmaValida(signature, "request-1", "999")).isFalse();
    assertThat(gateway.firmaValida(null, "request-1", "123")).isFalse();
  }
}
