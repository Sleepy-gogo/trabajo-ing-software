package edu.unse.sera.socio.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.membresia.persistence.NivelMembresiaRepository;
import edu.unse.sera.pagos.control.SuscripcionMercadoPagoService;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.socio.persistence.CambioSocioRepository;
import edu.unse.sera.socio.persistence.SocioRepository;
import edu.unse.sera.usuario.control.UsuarioService;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SocioServiceTest {
  @Mock private SocioRepository socios;
  @Mock private MembresiaRepository membresias;
  @Mock private NivelMembresiaRepository niveles;
  @Mock private CambioSocioRepository cambios;
  @Mock private UsuarioService usuarios;
  @Mock private PagoRepository pagos;
  @Mock private SuscripcionMercadoPagoService suscripcionesMercadoPago;

  private SocioService service;

  @BeforeEach
  void setUp() {
    service =
        new SocioService(
            socios, membresias, niveles, cambios, usuarios, pagos, suscripcionesMercadoPago);
  }

  @Test
  void registrarSocioNoCreaMembresiaNiPago() {
    UUID usuarioId = UUID.randomUUID();
    Usuario usuario = usuario(usuarioId);
    when(usuarios.buscar(usuarioId)).thenReturn(usuario);

    SocioDetalle detalle =
        service.registrar(usuarioId, RelacionUnse.EXTERNO, null, null, usuarioId);

    assertThat(detalle.membresiaId()).isNull();
    verify(socios).save(any(Socio.class));
    verify(membresias, never()).save(any());
    verify(pagos, never()).save(any());
  }

  @Test
  void contratarCreaMembresiaYPagoPendientesConTarifaExternaSiFaltaVerificacion() {
    UUID socioId = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    UUID nivelId = UUID.randomUUID();
    Usuario usuario = usuario(actor);
    Socio socio =
        new Socio(usuario, RelacionUnse.ESTUDIANTE, EstadoVerificacionUnse.PENDIENTE, null);
    NivelMembresia nivel = new NivelMembresia("General", "Acceso general");
    nivel.actualizar(
        "General",
        "Acceso general",
        Map.of(
            RelacionUnse.EXTERNO, new BigDecimal("1000.00"),
            RelacionUnse.ESTUDIANTE, new BigDecimal("500.00")),
        List.of("Pileta"),
        true);
    when(socios.findById(socioId)).thenReturn(Optional.of(socio));
    when(niveles.findById(nivelId)).thenReturn(Optional.of(nivel));
    when(membresias.save(any(Membresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.contratar(socioId, nivelId, MedioPago.MERCADO_PAGO, actor);

    assertThat(socio.getMembresia().getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
    ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
    verify(pagos).save(captor.capture());
    Pago pago = captor.getValue();
    assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
    assertThat(pago.getMonto()).isEqualByComparingTo("1000.00");
    assertThat(pago.getMembresia()).isSameAs(socio.getMembresia());
    assertThat(pago.getUsuario()).isSameAs(usuario);
  }

  @Test
  void altaDeSocioRechazaNivelParaEvitarContratacionImplicita() {
    UUID usuarioId = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                service.registrar(
                    usuarioId, RelacionUnse.EXTERNO, null, UUID.randomUUID(), usuarioId))
        .isInstanceOf(IllegalArgumentException.class);
    verify(socios, never()).save(any());
  }

  @Test
  void cancelarConservaCobrosAprobadosYCancelaSoloPendientes() {
    UUID actor = UUID.randomUUID();
    UUID membresiaId = UUID.randomUUID();
    Usuario titular = usuario(actor);
    Socio socio = new Socio(titular, RelacionUnse.EXTERNO, EstadoVerificacionUnse.PENDIENTE, null);
    ReflectionTestUtils.setField(socio, "id", UUID.randomUUID());
    var membresia = new Membresia(socio, new NivelMembresia("General", "Acceso"));
    ReflectionTestUtils.setField(membresia, "id", membresiaId);
    var aprobado =
        new Pago(
            ConceptoPago.CUOTA_MENSUAL,
            titular,
            MedioPago.EFECTIVO,
            new BigDecimal("1000.00"),
            membresia);
    aprobado.aprobar("SERA-1", OffsetDateTime.parse("2026-09-27T12:00:00Z"));
    var pendiente =
        new Pago(
            ConceptoPago.CUOTA_MENSUAL,
            titular,
            MedioPago.EFECTIVO,
            new BigDecimal("1000.00"),
            membresia);
    membresia.activarPorPago();
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(membresia));
    when(pagos.findAllByMembresiaId(membresiaId)).thenReturn(List.of(aprobado, pendiente));

    service.cancelar(membresiaId, "Solicitud del titular", actor);

    assertThat(aprobado.getEstado()).isEqualTo(EstadoPago.APROBADO);
    assertThat(pendiente.getEstado()).isEqualTo(EstadoPago.CANCELADO);
    assertThat(membresia.getEstado()).isEqualTo(EstadoMembresia.CANCELADA);
  }

  @Test
  void falloAlCancelarSuscripcionConservaLaMembresiaYElPagoPendiente() {
    UUID actor = UUID.randomUUID();
    UUID membresiaId = UUID.randomUUID();
    Socio socio =
        new Socio(usuario(actor), RelacionUnse.EXTERNO, EstadoVerificacionUnse.PENDIENTE, null);
    var membresia = new Membresia(socio, new NivelMembresia("General", "Acceso"));
    ReflectionTestUtils.setField(membresia, "id", membresiaId);
    when(membresias.bloquearPorId(membresiaId)).thenReturn(Optional.of(membresia));
    doThrow(new edu.unse.sera.pagos.control.MercadoPagoNoDisponibleException())
        .when(suscripcionesMercadoPago)
        .cancelarVigente(membresiaId);

    assertThatThrownBy(() -> service.cancelar(membresiaId, "Cancelar", actor))
        .isInstanceOf(edu.unse.sera.pagos.control.MercadoPagoNoDisponibleException.class);
    assertThat(membresia.getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
    verify(pagos, never()).findAllByMembresiaId(any());
    verify(cambios, never()).save(any());
  }

  private Usuario usuario(UUID id) {
    Usuario usuario =
        new Usuario(
            "Ada Lovelace",
            "ada@example.com",
            12345678,
            EstadoUsuario.ACTIVO,
            RolUsuario.USUARIO,
            "hash");
    ReflectionTestUtils.setField(usuario, "id", id);
    return usuario;
  }
}
