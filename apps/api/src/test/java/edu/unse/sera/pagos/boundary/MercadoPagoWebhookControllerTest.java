package edu.unse.sera.pagos.boundary;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.pagos.control.MercadoPagoGateway;
import edu.unse.sera.pagos.control.SuscripcionMercadoPagoService;
import edu.unse.sera.shared.config.SecurityConfig;
import edu.unse.sera.usuario.control.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MercadoPagoWebhookController.class)
@Import(SecurityConfig.class)
class MercadoPagoWebhookControllerTest {
  @Autowired private MockMvc mvc;
  @MockitoBean private MercadoPagoGateway mercadoPago;
  @MockitoBean private SuscripcionMercadoPagoService suscripciones;
  @MockitoBean private UsuarioService usuarios;
  @MockitoBean private edu.unse.sera.reserva.control.ReservaService reservas;

  @Test
  void aceptaWebhookFirmadoSinSesionNiCsrf() throws Exception {
    when(mercadoPago.firmaValida("firma", "request-1", "123")).thenReturn(true);

    mvc.perform(
            post("/api/webhooks/mercadopago")
                .param("data.id", "123")
                .param("type", "subscription_authorized_payment")
                .header("x-signature", "firma")
                .header("x-request-id", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"type\":\"subscription_authorized_payment\",\"data\":{\"id\":\"123\"}}"))
        .andExpect(status().isOk());

    verify(suscripciones).recibirFactura(123L);
  }

  @Test
  void rechazaFirmaInvalida() throws Exception {
    mvc.perform(
            post("/api/webhooks/mercadopago")
                .param("data.id", "123")
                .param("type", "subscription_authorized_payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"type\":\"subscription_authorized_payment\",\"data\":{\"id\":\"123\"}}"))
        .andExpect(status().isUnauthorized());

    verify(suscripciones, never()).recibirFactura(123L);
  }

  @Test
  void paymentFirmadoProcesaLaReservaSinSesion() throws Exception {
    when(mercadoPago.firmaValida("firma", "request-1", "123")).thenReturn(true);
    mvc.perform(
            post("/api/webhooks/mercadopago")
                .param("data.id", "123")
                .param("type", "payment")
                .header("x-signature", "firma")
                .header("x-request-id", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}"))
        .andExpect(status().isOk());
    verify(reservas).recibirPago(123L);
  }

  @Test
  void rechazaDatosQueNoCoincidenConLaUrlFirmada() throws Exception {
    when(mercadoPago.firmaValida("firma", "request-1", "123")).thenReturn(true);

    mvc.perform(
            post("/api/webhooks/mercadopago")
                .param("data.id", "123")
                .param("type", "subscription_authorized_payment")
                .header("x-signature", "firma")
                .header("x-request-id", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{"
                        + "\"type\":\"subscription_authorized_payment\",\"data\":{\"id\":\"999\"}}"))
        .andExpect(status().isBadRequest());

    verify(suscripciones, never()).recibirFactura(123L);
  }
}
