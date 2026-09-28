package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {
  @Mock private PagoRepository pagos;
  @Mock private MembresiaRepository membresias;
  @Mock private UsuarioRepository usuarios;
  @Mock private SuscripcionMercadoPagoRepository suscripciones;
  private PagoService service;
  private Usuario titular;
  private Membresia membresia;
  private UUID membresiaId;

  @BeforeEach
  void preparar() {
    service = new PagoService(pagos, membresias, usuarios, suscripciones);
    titular = usuario(RolUsuario.USUARIO);
    var socio = new Socio(titular, RelacionUnse.ESTUDIANTE, EstadoVerificacionUnse.PENDIENTE, null);
    var nivel = new NivelMembresia("General", "Acceso");
    nivel.actualizar(
        "General",
        "Acceso",
        Map.of(RelacionUnse.EXTERNO, new BigDecimal("1000.00")),
        List.of("Acceso"),
        true);
    membresia = new Membresia(socio, nivel);
    membresiaId = UUID.randomUUID();
    ReflectionTestUtils.setField(membresia, "id", membresiaId);
  }

  @Test
  void iniciaConTarifaExternaYGuardaAntesDeResponder() {
    UUID clave = UUID.randomUUID();
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(membresia));
    when(pagos.saveAndFlush(any(Pago.class)))
        .thenAnswer(
            invocation -> {
              Pago pago = invocation.getArgument(0);
              ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
              return pago;
            });

    var detalle = service.iniciarPagoCuota(membresiaId, titular.getId(), MedioPago.EFECTIVO, clave);

    assertThat(detalle.id()).isNotNull();
    assertThat(detalle.monto()).isEqualByComparingTo("1000.00");
    assertThat(detalle.estado()).isEqualTo(EstadoPago.PENDIENTE);
    verify(pagos).saveAndFlush(any(Pago.class));
  }

  @Test
  void mismaClaveDevuelveElPagoExistente() {
    Pago pago = pendiente(MedioPago.EFECTIVO);
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(membresia));
    when(pagos.findByUsuarioIdAndClaveSolicitud(titular.getId(), pago.getClaveSolicitud()))
        .thenReturn(Optional.of(pago));

    assertThat(
            service
                .iniciarPagoCuota(
                    membresiaId, titular.getId(), MedioPago.EFECTIVO, pago.getClaveSolicitud())
                .id())
        .isEqualTo(pago.getId());
    verify(pagos, never()).saveAndFlush(any(Pago.class));
  }

  @Test
  void aprobacionDuplicadaNoAgregaOtroMes() {
    Pago pago = pendiente(MedioPago.MERCADO_PAGO);
    when(pagos.findById(pago.getId())).thenReturn(Optional.of(pago));
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(membresia));
    OffsetDateTime fecha = OffsetDateTime.parse("2026-09-27T12:00:00Z");

    service.confirmarPago(pago.getId(), "mp-123", fecha);
    LocalDate vencimiento = membresia.getProximoVencimiento();
    service.confirmarPago(pago.getId(), "mp-123", fecha);

    assertThat(membresia.getProximoVencimiento()).isEqualTo(vencimiento);
    assertThat(pago.getAplicadoEn()).isNotNull();
    assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
  }

  @Test
  void cobroDeContratacionAnteriorQuedaParaRevision() {
    Pago pago = pendiente(MedioPago.MERCADO_PAGO);
    membresia.cancelar();
    membresia.renovarSolicitud(membresia.getNivelMembresia());
    when(pagos.findById(pago.getId())).thenReturn(Optional.of(pago));
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(membresia));

    service.confirmarPago(pago.getId(), "mp-456", OffsetDateTime.now());

    assertThat(pago.isRequiereRevision()).isTrue();
    assertThat(membresia.getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
    assertThat(membresia.getProximoVencimiento()).isNull();
  }

  @Test
  void soloAdminPuedeConfirmarEfectivo() {
    Pago pago = pendiente(MedioPago.EFECTIVO);
    when(usuarios.findById(titular.getId())).thenReturn(Optional.of(titular));

    assertThatThrownBy(() -> service.confirmarPagoEfectivo(pago.getId(), titular.getId()))
        .isInstanceOf(OperacionNoPermitidaException.class);
    verify(pagos, never()).findById(any());
  }

  private Pago pendiente(MedioPago medio) {
    Pago pago =
        new Pago(ConceptoPago.CUOTA_MENSUAL, titular, medio, new BigDecimal("1000.00"), membresia);
    ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
    return pago;
  }

  private Usuario usuario(RolUsuario rol) {
    Usuario usuario =
        new Usuario("Ada Lovelace", "ada@example.com", 12345678, EstadoUsuario.ACTIVO, rol, "hash");
    ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
    return usuario;
  }
}
