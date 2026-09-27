package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.membresia.control.MembresiaService;
import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.membresia.persistence.NivelMembresiaRepository;
import edu.unse.sera.pagos.entity.ConceptoPago;
import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  @Mock private NivelMembresiaRepository niveles;

  private PagoService service;

  @BeforeEach
  void setUp() {
    service = new PagoService(pagos, membresias, usuarios, new MembresiaService(niveles));
  }

  @Test
  void aprobarPagoActivaLaMembresiaPendiente() {
    Pago pago = pagoPendiente();
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    service.confirmarPago(pagoId, "comprobante-123");

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
    assertThat(pago.getMembresia().getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
    assertThat(pago.getMembresia().getProximoVencimiento()).isNotNull();
  }

  @Test
  void confirmarPagoSinMembresiaDevuelveDetalleConIdMembresiaNulo() {
    Pago pago = pagoPendiente();
    pago.setMembresia(null);
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    PagoDetalle detalle = service.confirmarPago(pagoId, "comprobante-reserva");

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
    assertThat(detalle.idMembresia()).isNull();
  }

  @Test
  void rechazarPagoConservaMembresiaPendiente() {
    Pago pago = pagoPendiente();
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    service.rechazarPago(pagoId);

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
    assertThat(pago.getMembresia().getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
  }

  @Test
  void aprobarCuotaPosteriorConservaMembresiaActiva() {
    Pago pago = pagoPendiente();
    pago.getMembresia().activarPorPago();
    LocalDate vencimiento = LocalDate.now().plusMonths(2);
    pago.getMembresia().setProximoVencimiento(vencimiento);
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    service.confirmarPago(pagoId, "comprobante-456");

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
    assertThat(pago.getMembresia().getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
    assertThat(pago.getMembresia().getProximoVencimiento()).isEqualTo(vencimiento.plusMonths(1));
  }

  @Test
  void aprobarPagoReactivaMembresiaVencida() {
    Pago pago = pagoPendiente();
    pago.getMembresia().activarPorPago();
    pago.getMembresia().setProximoVencimiento(LocalDate.now().minusDays(1));
    pago.getMembresia().cambiarEstado(EstadoMembresia.VENCIDA);
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    service.confirmarPago(pagoId, "comprobante-vencida");

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
    assertThat(pago.getMembresia().getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
    assertThat(pago.getMembresia().getProximoVencimiento()).isAfter(LocalDate.now());
  }

  @Test
  void aprobarPagoReactivaMembresiaSuspendida() {
    Pago pago = pagoPendiente();
    pago.getMembresia().activarPorPago();
    pago.getMembresia().cambiarEstado(EstadoMembresia.SUSPENDIDA);
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    service.confirmarPago(pagoId, "comprobante-suspendida");

    assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
    assertThat(pago.getMembresia().getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
    assertThat(pago.getMembresia().getProximoVencimiento()).isAfter(LocalDate.now());
  }

  @Test
  void noApruebaPagoDeMembresiaCancelada() {
    Pago pago = pagoPendiente();
    pago.getMembresia().cancelar();
    UUID pagoId = UUID.randomUUID();
    when(pagos.findById(pagoId)).thenReturn(Optional.of(pago));

    assertThatThrownBy(() -> service.confirmarPago(pagoId, "comprobante-123"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
  }

  @Test
  void noCreaOtroPagoMientrasExisteUnoPendiente() {
    Pago existente = pagoPendiente();
    UUID membresiaId = UUID.randomUUID();
    UUID usuarioId = existente.getUsuario().getId();
    when(membresias.findById(membresiaId)).thenReturn(Optional.of(existente.getMembresia()));
    when(usuarios.findById(usuarioId)).thenReturn(Optional.of(existente.getUsuario()));
    when(membresias.getReferenceById(membresiaId)).thenReturn(existente.getMembresia());
    when(pagos.existsByMembresiaIdAndEstado(membresiaId, EstadoPago.PENDIENTE)).thenReturn(true);

    assertThatThrownBy(
            () -> service.iniciarPagoCuota(membresiaId, usuarioId, MedioPago.MERCADO_PAGO))
        .isInstanceOf(IllegalStateException.class);
    verify(pagos, never()).save(any());
  }

  private Pago pagoPendiente() {
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
    return new Pago(
        ConceptoPago.CUOTA_MENSUAL,
        usuario,
        MedioPago.MERCADO_PAGO,
        new BigDecimal("1000.00"),
        membresia);
  }
}
