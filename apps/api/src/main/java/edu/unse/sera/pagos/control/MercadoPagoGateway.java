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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
  private final ObjectMapper mapper;
  private final HttpClient http;
  private final PreapprovalClient preapprovals = new PreapprovalClient();
  private final PaymentClient payments = new PaymentClient();

  public MercadoPagoGateway(
      @Value("${mercadopago.access-token:}") String accessToken,
      @Value("${mercadopago.webhook-secret:}") String webhookSecret,
      @Value("${mercadopago.back-url:}") String backUrl,
      ObjectMapper mapper) {
    this.accessToken = accessToken;
    this.webhookSecret = webhookSecret;
    this.backUrl = backUrl;
    this.mapper = mapper;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    if (!accessToken.isBlank()) {
      MercadoPagoConfig.setAccessToken(accessToken);
    }
  }

  public Preapproval crear(UUID referencia, String email, String nivel, BigDecimal monto) {
    requerirCredenciales();
    if (!backUrl.startsWith("https://")) {
      throw new IllegalStateException("Configurá MP_BACK_URL con una URL HTTPS pública.");
    }
    var recurrencia =
        PreApprovalAutoRecurringCreateRequest.builder()
            .frequency(1)
            .frequencyType("months")
            .currencyId("ARS")
            .transactionAmount(monto)
            .build();
    var solicitud =
        PreapprovalCreateRequest.builder()
            .payerEmail(email)
            .reason("Membresía SERA: " + nivel)
            .externalReference(referencia.toString())
            .backUrl(backUrl)
            .status("pending")
            .autoRecurring(recurrencia)
            .build();
    try {
      return preapprovals.create(solicitud);
    } catch (MPException | MPApiException e) {
      throw new MercadoPagoNoDisponibleException(e);
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
      if (json.path("id").asLong() != id
          || json.path("preapproval_id").asText().isBlank()
          || json.path("payment").path("id").asLong() <= 0) {
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
