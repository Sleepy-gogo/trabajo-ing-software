package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preapproval.Preapproval;
import com.mercadopago.resources.preapproval.PreapprovalAutoRecurring;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.entity.SuscripcionMercadoPago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SuscripcionMercadoPagoServiceTest {
  @Mock private SuscripcionMercadoPagoRepository suscripciones;
  @Mock private MembresiaRepository membresias;
  @Mock private PagoRepository pagos;
  @Mock private PagoService pagoService;
  @Mock private MercadoPagoGateway mercadoPago;
  @Mock private Payment pagoRemoto;
  @Mock private Preapproval preapproval;
  @Mock private PreapprovalAutoRecurring recurrencia;

  private SuscripcionMercadoPagoService service;
  private SuscripcionMercadoPago suscripcion;
  private Pago pagoInicial;

  @BeforeEach
  void preparar() {
    service =
        new SuscripcionMercadoPagoService(
            suscripciones, membresias, pagos, pagoService, mercadoPago);
    Usuario usuario =
        new Usuario(
            "Ada Lovelace",
            "ada@example.com",
            12345678,
            EstadoUsuario.ACTIVO,
            RolUsuario.USUARIO,
            "hash");
    ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
    Socio socio = new Socio(usuario, RelacionUnse.EXTERNO, EstadoVerificacionUnse.PENDIENTE, null);
    Membresia membresia = new Membresia(socio, new NivelMembresia("General", "Acceso"));
    pagoInicial =
        new Pago(
            ConceptoPago.CUOTA_MENSUAL,
            usuario,
            MedioPago.MERCADO_PAGO,
            new BigDecimal("1000.00"),
            membresia);
    ReflectionTestUtils.setField(pagoInicial, "id", UUID.randomUUID());
    suscripcion = new SuscripcionMercadoPago(membresia, pagoInicial);
    suscripcion.vincular("preapproval-123", "https://www.mercadopago.com.ar/checkout");
  }

  @Test
  void facturaAprobadaConfirmaUnaCuotaCorrelacionada() {
    prepararFacturaAprobada();

    service.recibirFactura(123L);

    assertThat(pagoInicial.getMercadoPagoPaymentId()).isEqualTo("456");
    verify(pagoService).confirmarPago(pagoInicial.getId(), "456", pagoRemoto.getDateApproved());
  }

  @Test
  void titularIniciaSuscripcionConElImporteDelPagoPendiente() {
    UUID membresiaId = UUID.randomUUID();
    ReflectionTestUtils.setField(suscripcion.getMembresia(), "id", membresiaId);
    UUID titular = pagoInicial.getUsuario().getId();
    when(membresias.findById(membresiaId)).thenReturn(Optional.of(suscripcion.getMembresia()));
    when(pagos.findAllByMembresiaId(membresiaId)).thenReturn(java.util.List.of(pagoInicial));
    var creada = new java.util.concurrent.atomic.AtomicReference<SuscripcionMercadoPago>();
    when(suscripciones.saveAndFlush(any(SuscripcionMercadoPago.class)))
        .thenAnswer(
            invocation -> {
              SuscripcionMercadoPago local = invocation.getArgument(0);
              creada.set(local);
              return local;
            });
    when(mercadoPago.crear(
            any(UUID.class),
            org.mockito.ArgumentMatchers.eq("ada@example.com"),
            org.mockito.ArgumentMatchers.eq("General"),
            org.mockito.ArgumentMatchers.eq(new BigDecimal("1000.00"))))
        .thenReturn(preapproval);
    when(preapproval.getId()).thenReturn("preapproval-123");
    when(preapproval.getInitPoint()).thenReturn("https://www.mercadopago.com.ar/checkout");
    when(preapproval.getExternalReference())
        .thenAnswer(invocation -> creada.get().getId().toString());
    when(preapproval.getAutoRecurring()).thenReturn(recurrencia);
    when(recurrencia.getCurrencyId()).thenReturn("ARS");
    when(recurrencia.getFrequency()).thenReturn(1);
    when(recurrencia.getFrequencyType()).thenReturn("months");
    when(recurrencia.getTransactionAmount()).thenReturn(new BigDecimal("1000.00"));

    assertThat(service.iniciar(membresiaId, titular).checkoutUrl())
        .isEqualTo("https://www.mercadopago.com.ar/checkout");
  }

  @Test
  void otroUsuarioNoPuedeIniciarLaSuscripcion() {
    UUID membresiaId = UUID.randomUUID();
    when(membresias.findById(membresiaId)).thenReturn(Optional.of(suscripcion.getMembresia()));

    assertThatThrownBy(() -> service.iniciar(membresiaId, UUID.randomUUID()))
        .isInstanceOf(edu.unse.sera.shared.exception.OperacionNoPermitidaException.class);
    verify(mercadoPago, never())
        .crear(any(UUID.class), any(String.class), any(String.class), any(BigDecimal.class));
  }

  @Test
  void rechazaUnaFacturaConImporteDistinto() {
    when(mercadoPago.obtenerFactura(123L))
        .thenReturn(
            new FacturaMercadoPago(
                123L,
                "preapproval-123",
                suscripcion.getId().toString(),
                "ARS",
                new BigDecimal("2000.00"),
                456L));
    when(suscripciones.findByPreapprovalId("preapproval-123")).thenReturn(Optional.of(suscripcion));

    assertThatThrownBy(() -> service.recibirFactura(123L))
        .isInstanceOf(IllegalStateException.class);
    verify(mercadoPago, never()).obtenerPago(456L);
  }

  @Test
  void pideReintentoSiFacturaLlegaAntesDeGuardarLaSuscripcion() {
    when(mercadoPago.obtenerFactura(123L))
        .thenReturn(
            new FacturaMercadoPago(
                123L,
                "preapproval-123",
                suscripcion.getId().toString(),
                "ARS",
                new BigDecimal("1000.00"),
                456L));

    assertThatThrownBy(() -> service.recibirFactura(123L))
        .isInstanceOf(MercadoPagoNoDisponibleException.class);
  }

  @Test
  void reintentoDelWebhookNoDuplicaLaCuota() {
    prepararFacturaAprobada();
    when(pagos.findByMercadoPagoPaymentId("456")).thenReturn(Optional.of(pagoInicial));

    service.recibirFactura(123L);

    verify(pagoService, never())
        .confirmarPago(pagoInicial.getId(), "456", pagoRemoto.getDateApproved());
  }

  private void prepararFacturaAprobada() {
    when(mercadoPago.obtenerFactura(123L))
        .thenReturn(
            new FacturaMercadoPago(
                123L,
                "preapproval-123",
                suscripcion.getId().toString(),
                "ARS",
                new BigDecimal("1000.00"),
                456L));
    when(suscripciones.findByPreapprovalId("preapproval-123")).thenReturn(Optional.of(suscripcion));
    when(mercadoPago.obtenerPago(456L)).thenReturn(pagoRemoto);
    when(pagoRemoto.getId()).thenReturn(456L);
    when(pagoRemoto.getCurrencyId()).thenReturn("ARS");
    when(pagoRemoto.getTransactionAmount()).thenReturn(new BigDecimal("1000.00"));
    when(pagoRemoto.getStatus()).thenReturn("approved");
    when(pagoRemoto.getDateApproved()).thenReturn(OffsetDateTime.parse("2026-09-27T12:00:00Z"));
    when(pagoRemoto.getCollectorId()).thenReturn(789L);
    when(mercadoPago.obtenerSuscripcion("preapproval-123")).thenReturn(preapproval);
    when(preapproval.getId()).thenReturn("preapproval-123");
    when(preapproval.getExternalReference()).thenReturn(suscripcion.getId().toString());
    when(preapproval.getAutoRecurring()).thenReturn(recurrencia);
    when(recurrencia.getCurrencyId()).thenReturn("ARS");
    when(recurrencia.getFrequency()).thenReturn(1);
    when(recurrencia.getFrequencyType()).thenReturn("months");
    when(recurrencia.getTransactionAmount()).thenReturn(new BigDecimal("1000.00"));
    when(preapproval.getCollectorId()).thenReturn(789L);
  }
}
