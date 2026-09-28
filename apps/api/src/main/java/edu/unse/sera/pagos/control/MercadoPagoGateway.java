package edu.unse.sera.pagos.control;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalUpdateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preapproval.Preapproval;
import com.mercadopago.webhook.WebhookSignatureValidator;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MercadoPagoGateway {
  private final String accessToken;
  private final String webhookSecret;
  private final String backUrl;
  private final String testPayerEmail;
  private final ObjectMapper mapper;
  private final HttpClient http;
  private final PreapprovalClient preapprovals = new PreapprovalClient();
  private final PaymentClient payments = new PaymentClient();

  public MercadoPagoGateway(
      @Value("${mercadopago.access-token:}") String accessToken,
      @Value("${mercadopago.webhook-secret:}") String webhookSecret,
      @Value("${mercadopago.back-url:}") String backUrl,
      @Value("${mercadopago.test-payer-email:}") String testPayerEmail,
      ObjectMapper mapper) {
    this.accessToken = accessToken;
    this.webhookSecret = webhookSecret;
    this.backUrl = backUrl;
    this.testPayerEmail = testPayerEmail.trim();
    this.mapper = mapper;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    if (!accessToken.isBlank()) {
      MercadoPagoConfig.setAccessToken(accessToken);
    }
  }

  public Preapproval crear(UUID referencia, String email, String nivel, BigDecimal monto) {
    validarConfiguracion();
    var recurrencia =
        PreApprovalAutoRecurringCreateRequest.builder()
            .frequency(1)
            .frequencyType("months")
            .currencyId("ARS")
            .transactionAmount(monto)
            .build();
    String comprador;
    try {
      comprador = emailComprador(email);
    } catch (RuntimeException e) {
      // No se envió el alta: corregir la configuración permite reintentar sin duplicar.
      throw new MercadoPagoSolicitudRechazadaException(e.getMessage(), e);
    }
    var solicitud =
        PreapprovalCreateRequest.builder()
            .payerEmail(comprador)
            .reason("Membresía SERA: " + nivel)
            .externalReference(referencia.toString())
            .backUrl(backUrl)
            .status("pending")
            .autoRecurring(recurrencia)
            .build();
    try {
      return preapprovals.create(solicitud);
    } catch (MPApiException e) {
      if (e.getStatusCode() == 400
          || e.getStatusCode() == 422
          || e.getStatusCode() == 401
          || e.getStatusCode() == 403) {
        String mensaje =
            "Mercado Pago rechazó la solicitud. Revisá la cuenta compradora y la configuración del vendedor.";
        if (e.getApiResponse() != null
            && e.getApiResponse().getContent() != null
            && e.getApiResponse()
                .getContent()
                .contains("Both payer and collector must be real or test users")) {
          mensaje =
              "Mercado Pago requiere comprador y vendedor del mismo entorno. "
                  + "En esta demo, configurá el email del comprador de prueba; "
                  + "el email de SERA puede ser distinto.";
        }
        throw new MercadoPagoSolicitudRechazadaException(mensaje, e);
      }
      throw new MercadoPagoNoDisponibleException(e);
    } catch (MPException e) {
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  String emailComprador(String email) {
    if (testPayerEmail.isBlank()) {
      return email;
    }
    if (!testPayerEmail.endsWith("@testuser.com")) {
      throw new IllegalStateException(
          "MP_TEST_PAYER_EMAIL debe ser el email del comprador de prueba de Mercado Pago.");
    }
    var request =
        HttpRequest.newBuilder(URI.create("https://api.mercadopago.com/users/me"))
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new MercadoPagoNoDisponibleException();
      }
      boolean testSeller = false;
      for (var tag : mapper.readTree(response.body()).path("tags")) {
        if ("test_user".equals(tag.asText())) {
          testSeller = true;
        }
      }
      if (!testSeller) {
        throw new IllegalStateException(
            "MP_TEST_PAYER_EMAIL solo puede usarse con un vendedor de prueba.");
      }
      return testPayerEmail;
    } catch (IOException e) {
      throw new MercadoPagoNoDisponibleException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public String crearCheckout(
      UUID pagoId,
      UUID reservaId,
      String espacio,
      BigDecimal monto,
      java.time.OffsetDateTime vence) {
    validarConfiguracion();
    String retorno = URI.create(backUrl).resolve("/app/reservations/" + reservaId).toString();
    var item =
        com.mercadopago.client.preference.PreferenceItemRequest.builder()
            .id(reservaId.toString())
            .title("Reserva SERA: " + espacio)
            .quantity(1)
            .currencyId("ARS")
            .unitPrice(monto)
            .build();
    var solicitud =
        com.mercadopago.client.preference.PreferenceRequest.builder()
            .items(List.of(item))
            .externalReference(pagoId.toString())
            .backUrls(
                com.mercadopago.client.preference.PreferenceBackUrlsRequest.builder()
                    .success(retorno)
                    .pending(retorno)
                    .failure(retorno)
                    .build())
            .autoReturn("approved")
            .expires(true)
            .expirationDateTo(vence)
            .build();
    try {
      var preferencia = new com.mercadopago.client.preference.PreferenceClient().create(solicitud);
      if (preferencia.getInitPoint() == null
          || !preferencia.getInitPoint().startsWith("https://")) {
        throw new MercadoPagoNoDisponibleException();
      }
      return preferencia.getInitPoint();
    } catch (MPException | MPApiException e) {
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public void validarConfiguracion() {
    requerirCredenciales();
    if (!backUrl.startsWith("https://")) {
      throw new IllegalStateException("Configurá MP_BACK_URL con una URL HTTPS pública.");
    }
  }

  public Preapproval obtenerSuscripcion(String id) {
    requerirCredenciales();
    try {
      return preapprovals.get(id);
    } catch (MPException | MPApiException e) {
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public void cancelarSuscripcion(String id) {
    requerirCredenciales();
    try {
      preapprovals.update(id, PreapprovalUpdateRequest.builder().status("canceled").build());
    } catch (MPException | MPApiException e) {
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public Payment obtenerPago(long id) {
    requerirCredenciales();
    try {
      return payments.get(id);
    } catch (MPException | MPApiException e) {
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public FacturaMercadoPago obtenerFactura(long id) {
    requerirCredenciales();
    var request =
        HttpRequest.newBuilder(URI.create("https://api.mercadopago.com/authorized_payments/" + id))
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new MercadoPagoNoDisponibleException();
      }
      JsonNode json = mapper.readTree(response.body());
      if (json.path("id").asLong() != id || json.path("preapproval_id").asText().isBlank()) {
        throw new MercadoPagoNoDisponibleException();
      }
      return new FacturaMercadoPago(
          json.path("id").asLong(),
          json.path("preapproval_id").asText(),
          json.path("external_reference").asText(),
          json.path("currency_id").asText(),
          new BigDecimal(json.path("transaction_amount").asText()),
          json.path("payment").path("id").asLong());
    } catch (IOException e) {
      throw new MercadoPagoNoDisponibleException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public record PaginaFacturas(List<Long> ids, int total, int resultados) {}

  public PaginaFacturas buscarFacturas(String preapprovalId, int offset) {
    requerirCredenciales();
    String query = URLEncoder.encode(preapprovalId, StandardCharsets.UTF_8);
    var request =
        HttpRequest.newBuilder(
                URI.create(
                    "https://api.mercadopago.com/authorized_payments/search?preapproval_id="
                        + query
                        + "&offset="
                        + offset))
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new MercadoPagoNoDisponibleException();
      }
      JsonNode json = mapper.readTree(response.body());
      if (!json.path("results").isArray() || json.path("paging").path("total").asInt(-1) < 0) {
        throw new MercadoPagoNoDisponibleException();
      }
      List<Long> ids = new ArrayList<>();
      int resultados = 0;
      for (JsonNode item : json.path("results")) {
        if (item.path("id").asLong() <= 0
            || !preapprovalId.equals(item.path("preapproval_id").asText())) {
          throw new MercadoPagoNoDisponibleException();
        }
        resultados++;
        if (item.path("payment").path("id").asLong() > 0) {
          ids.add(item.path("id").asLong());
        }
      }
      return new PaginaFacturas(ids, json.path("paging").path("total").asInt(), resultados);
    } catch (IOException e) {
      throw new MercadoPagoNoDisponibleException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MercadoPagoNoDisponibleException(e);
    }
  }

  public boolean firmaValida(String signature, String requestId, String dataId) {
    if (webhookSecret.isBlank() || dataId == null || dataId.isBlank()) {
      return false;
    }
    try {
      WebhookSignatureValidator.validate(signature, requestId, dataId, webhookSecret);
      return true;
    } catch (MPInvalidWebhookSignatureException e) {
      return false;
    }
  }

  private void requerirCredenciales() {
    if (accessToken.isBlank()) {
      throw new IllegalStateException("Configurá MP_ACCESS_TOKEN en el backend.");
    }
  }
}
