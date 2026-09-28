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
              "token-demo", "", "https://sera.example/app/payments", "", new ObjectMapper());
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
    var gateway = new MercadoPagoGateway("", "secreto", "", "", new ObjectMapper());

    assertThat(gateway.firmaValida(signature, "request-1", "123")).isTrue();
    assertThat(gateway.firmaValida(signature, "request-1", "999")).isFalse();
    assertThat(gateway.firmaValida(null, "request-1", "123")).isFalse();
  }

  @Test
  void distingueRechazoDefinitivoDeErrorIncierto() throws Exception {
    var rechazo =
        new com.mercadopago.exceptions.MPApiException(
            "rechazo",
            new com.mercadopago.net.MPResponse(
                400,
                java.util.Map.of(),
                "{\"message\":\"Both payer and collector must be real or test users\"}"));
    try (var clientes =
        org.mockito.Mockito.mockConstruction(
            com.mercadopago.client.preapproval.PreapprovalClient.class,
            (cliente, contexto) ->
                org.mockito.Mockito.when(
                        cliente.create(
                            org.mockito.ArgumentMatchers.any(
                                com.mercadopago.client.preapproval.PreapprovalCreateRequest.class)))
                    .thenThrow(rechazo))) {
      var gateway =
          new MercadoPagoGateway("token", "", "https://sera.example", "", new ObjectMapper());
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  gateway.crear(
                      java.util.UUID.randomUUID(),
                      "persona@example.com",
                      "General",
                      java.math.BigDecimal.TEN))
          .isInstanceOf(MercadoPagoSolicitudRechazadaException.class)
          .hasMessageContaining("mismo entorno");
    }
    var error =
        new com.mercadopago.exceptions.MPApiException(
            "error", new com.mercadopago.net.MPResponse(500, java.util.Map.of(), "{}"));
    try (var clientes =
        org.mockito.Mockito.mockConstruction(
            com.mercadopago.client.preapproval.PreapprovalClient.class,
            (cliente, contexto) ->
                org.mockito.Mockito.when(
                        cliente.create(
                            org.mockito.ArgumentMatchers.any(
                                com.mercadopago.client.preapproval.PreapprovalCreateRequest.class)))
                    .thenThrow(error))) {
      var gateway =
          new MercadoPagoGateway("token", "", "https://sera.example", "", new ObjectMapper());
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  gateway.crear(
                      java.util.UUID.randomUUID(),
                      "persona@example.com",
                      "General",
                      java.math.BigDecimal.TEN))
          .isInstanceOf(MercadoPagoNoDisponibleException.class);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void compradorDePruebaNoPuedeUsarseConVendedorReal() throws Exception {
    var gateway =
        new MercadoPagoGateway(
            "token", "", "https://sera.example", "test_user_123@testuser.com", new ObjectMapper());
    var http = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
    java.net.http.HttpResponse<String> response =
        org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
    org.springframework.test.util.ReflectionTestUtils.setField(gateway, "http", http);
    org.mockito.Mockito.when(
            http.send(
                org.mockito.ArgumentMatchers.any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.any(java.net.http.HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    org.mockito.Mockito.when(response.statusCode()).thenReturn(200);
    org.mockito.Mockito.when(response.body()).thenReturn("{\"tags\":[\"normal\"]}");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> gateway.emailComprador("sera@example.com"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("vendedor de prueba");
    org.mockito.Mockito.when(response.body()).thenReturn("{\"tags\":[\"test_user\"]}");
    assertThat(gateway.emailComprador("sera@example.com")).isEqualTo("test_user_123@testuser.com");
    assertThat(
            new MercadoPagoGateway("token", "", "https://sera.example", "", new ObjectMapper())
                .emailComprador("sera@example.com"))
        .isEqualTo("sera@example.com");
  }

  @Test
  @SuppressWarnings("unchecked")
  void buscaFacturasConPaginacionCompatibleYRechazaOtraSuscripcion() throws Exception {
    var gateway = new MercadoPagoGateway("token", "", "", "", new ObjectMapper());
    var http = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
    java.net.http.HttpResponse<String> response =
        org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
    org.springframework.test.util.ReflectionTestUtils.setField(gateway, "http", http);
    var request = org.mockito.ArgumentCaptor.forClass(java.net.http.HttpRequest.class);
    org.mockito.Mockito.when(
            http.send(
                request.capture(),
                org.mockito.ArgumentMatchers.any(java.net.http.HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    org.mockito.Mockito.when(response.statusCode()).thenReturn(200);
    org.mockito.Mockito.when(response.body())
        .thenReturn(
            """
        {"paging":{"total":13},"results":[
          {"id":42,"preapproval_id":"subscription","payment":{"id":99}}
        ]}
        """);
    assertThat(gateway.buscarFacturas("subscription", 12).ids()).containsExactly(42L);
    assertThat(request.getValue().uri().getQuery())
        .isEqualTo("preapproval_id=subscription&offset=12");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> gateway.buscarFacturas("other", 0))
        .isInstanceOf(MercadoPagoNoDisponibleException.class);
  }
}
