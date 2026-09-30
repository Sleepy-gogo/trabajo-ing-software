package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class SuscripcionMercadoPagoServiceTest {
  @Mock private SuscripcionMercadoPagoRepository suscripciones;
  @Mock private MembresiaRepository membresias;
  @Mock private PagoRepository pagos;
  @Mock private PagoService pagoService;
  @Mock private MercadoPagoGateway mercadoPago;
  @Mock private TransactionTemplate transacciones;
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
            suscripciones, membresias, pagos, pagoService, mercadoPago, transacciones);
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
    ReflectionTestUtils.setField(suscripcion, "id", UUID.randomUUID());
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
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(suscripcion.getMembresia()));
    when(transacciones.execute(any()))
        .thenAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(mock(TransactionStatus.class));
            });
    when(pagos.findAllByMembresiaId(membresiaId)).thenReturn(java.util.List.of(pagoInicial));
    var creada = new java.util.concurrent.atomic.AtomicReference<SuscripcionMercadoPago>();
    when(suscripciones.saveAndFlush(any(SuscripcionMercadoPago.class)))
        .thenAnswer(
            invocation -> {
              SuscripcionMercadoPago local = invocation.getArgument(0);
              ReflectionTestUtils.setField(local, "id", UUID.randomUUID());
              creada.set(local);
              return local;
            });
    when(suscripciones.findById(any(UUID.class)))
        .thenAnswer(invocation -> Optional.of(creada.get()));
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
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(suscripcion.getMembresia()));
    when(transacciones.execute(any()))
        .thenAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(mock(TransactionStatus.class));
            });

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
  void webhookEnlazaSuscripcionCreadaAntesDeRecibirRespuesta() {
    var local = new SuscripcionMercadoPago(suscripcion.getMembresia(), pagoInicial);
    ReflectionTestUtils.setField(local, "id", UUID.randomUUID());
    when(suscripciones.findById(local.getId())).thenReturn(Optional.of(local));
    when(mercadoPago.obtenerSuscripcion("preapproval-nueva")).thenReturn(preapproval);
    when(preapproval.getId()).thenReturn("preapproval-nueva");
    when(preapproval.getExternalReference()).thenReturn(local.getId().toString());
    when(preapproval.getInitPoint()).thenReturn("https://www.mercadopago.com.ar/checkout");
    when(preapproval.getStatus()).thenReturn("pending");
    when(preapproval.getAutoRecurring()).thenReturn(recurrencia);
    when(recurrencia.getCurrencyId()).thenReturn("ARS");
    when(recurrencia.getFrequency()).thenReturn(1);
    when(recurrencia.getFrequencyType()).thenReturn("months");
    when(recurrencia.getTransactionAmount()).thenReturn(new BigDecimal("1000.00"));

    service.recibirSuscripcion("preapproval-nueva");

    assertThat(local.getPreapprovalId()).isEqualTo("preapproval-nueva");
    assertThat(local.getEstado()).isEqualTo("pending");
  }

  @Test
  void noCancelaLocalmenteUnaSuscripcionRemotaIncierta() {
    UUID membresiaId = UUID.randomUUID();
    var incierta = new SuscripcionMercadoPago(suscripcion.getMembresia(), pagoInicial);
    when(suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId))
        .thenReturn(java.util.List.of(incierta));

    assertThatThrownBy(() -> service.cancelarVigente(membresiaId))
        .isInstanceOf(IllegalStateException.class);
    verify(mercadoPago, never()).cancelarSuscripcion(any(String.class));
  }

  @Test
  void reintentoDelWebhookNoDuplicaLaCuota() {
    prepararFacturaAprobada();
    when(pagos.findByMercadoPagoPaymentId("456")).thenReturn(Optional.of(pagoInicial));

    service.recibirFactura(123L);

    verify(pagoService, never())
        .confirmarPago(pagoInicial.getId(), "456", pagoRemoto.getDateApproved());
  }

  @Test
  void cuotaTardiaConservaContratacionYNivelOriginalTrasVolverAContratar() {
    prepararFacturaAprobada();
    UUID contratacionAnterior = pagoInicial.getContratacionId();
    pagoInicial.aprobar("cobro-anterior", OffsetDateTime.parse("2026-09-27T11:00:00Z"));
    var membresia = suscripcion.getMembresia();
    membresia.cancelar();
    var nuevoNivel = new NivelMembresia("Nuevo nivel", "Otros beneficios");
    nuevoNivel.actualizar(
        "Nuevo nivel",
        "Otros beneficios",
        java.util.Map.of(RelacionUnse.EXTERNO, new BigDecimal("2000.00")),
        java.util.List.of("Acceso al nuevo nivel"),
        true);
    membresia.renovarSolicitud(nuevoNivel);
    when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
    var nuevaCuota = org.mockito.ArgumentCaptor.forClass(Pago.class);

    service.recibirFactura(123L);

    verify(pagos).saveAndFlush(nuevaCuota.capture());
    assertThat(nuevaCuota.getValue().getContratacionId()).isEqualTo(contratacionAnterior);
    assertThat(nuevaCuota.getValue().getContratacionId())
        .isNotEqualTo(membresia.getContratacionId());
    assertThat(nuevaCuota.getValue().getNivelNombreAplicado()).isEqualTo("General");
    assertThat(membresia.getProximoVencimiento()).isNull();
  }

  @Test
  void segundoCobroDeLaMismaFacturaQuedaAprobadoParaRevision() {
    prepararFacturaAprobada();
    pagoInicial.aprobar("cobro-anterior", OffsetDateTime.parse("2026-09-27T11:00:00Z"));
    when(pagos.findByMercadoPagoFacturaIdAndEstado(
            123L, edu.unse.sera.pagos.entity.EstadoPago.APROBADO))
        .thenReturn(Optional.of(pagoInicial));
    when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
    var nuevo = org.mockito.ArgumentCaptor.forClass(Pago.class);

    service.recibirFactura(123L);

    verify(pagos).saveAndFlush(nuevo.capture());
    assertThat(nuevo.getValue().isRequiereRevision()).isTrue();
    assertThat(nuevo.getValue().getEstado())
        .isEqualTo(edu.unse.sera.pagos.entity.EstadoPago.APROBADO);
    verify(pagoService, never()).confirmarPago(any(), any(), any());
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

  @Test
  void rechazoDefinitivoSePuedeReintentarPeroTimeoutNo() {
    var membresia = suscripcion.getMembresia();
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(membresia, "id", id);
    ReflectionTestUtils.setField(suscripcion, "preapprovalId", null);
    suscripcion.actualizarEstado("rejected");
    when(membresias.bloquearPorId(id)).thenReturn(Optional.of(membresia));
    when(pagos.findAllByMembresiaId(id)).thenReturn(java.util.List.of(pagoInicial));
    when(suscripciones.findByPagoInicialId(pagoInicial.getId()))
        .thenReturn(Optional.of(suscripcion));
    when(suscripciones.findById(suscripcion.getId())).thenReturn(Optional.of(suscripcion));
    when(transacciones.execute(any()))
        .thenAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(mock(TransactionStatus.class));
            });
    when(mercadoPago.crear(any(), any(), any(), any()))
        .thenThrow(new MercadoPagoSolicitudRechazadaException("Comprador inválido", null))
        .thenThrow(new MercadoPagoNoDisponibleException());
    UUID titular = pagoInicial.getUsuario().getId();
    assertThatThrownBy(() -> service.iniciar(id, titular))
        .isInstanceOf(MercadoPagoSolicitudRechazadaException.class);
    assertThat(suscripcion.getEstado()).isEqualTo("rejected");
    assertThatThrownBy(() -> service.iniciar(id, titular))
        .isInstanceOf(MercadoPagoNoDisponibleException.class);
    assertThat(suscripcion.getEstado()).isEqualTo("pending");
    assertThatThrownBy(() -> service.iniciar(id, titular))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("incierto");
    verify(mercadoPago, org.mockito.Mockito.times(2)).crear(any(), any(), any(), any());
  }

  @Test
  void cancelacionRemotaYaCompletadaPermiteRecuperarLaCancelacionLocal() {
    UUID membresiaId = UUID.randomUUID();
    when(suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresiaId))
        .thenReturn(java.util.List.of(suscripcion));
    when(mercadoPago.obtenerSuscripcion("preapproval-123")).thenReturn(preapproval);
    when(preapproval.getId()).thenReturn("preapproval-123");
    when(preapproval.getExternalReference()).thenReturn(suscripcion.getId().toString());
    when(preapproval.getAutoRecurring()).thenReturn(recurrencia);
    when(recurrencia.getCurrencyId()).thenReturn("ARS");
    when(recurrencia.getFrequency()).thenReturn(1);
    when(recurrencia.getFrequencyType()).thenReturn("months");
    when(recurrencia.getTransactionAmount()).thenReturn(new BigDecimal("1000.00"));
    when(preapproval.getStatus()).thenReturn("cancelled");

    service.cancelarVigente(membresiaId);

    assertThat(suscripcion.getEstado()).isEqualTo("canceled");
    verify(mercadoPago, never()).cancelarSuscripcion(any());
    suscripcion.actualizarEstado("cancelled");
    assertThat(suscripcion.getEstado()).isEqualTo("canceled");
    suscripcion.actualizarEstado("authorized");
    assertThat(suscripcion.getEstado()).isEqualTo("canceled");
  }
}
