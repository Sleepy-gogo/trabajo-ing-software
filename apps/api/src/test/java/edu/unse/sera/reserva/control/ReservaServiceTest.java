package edu.unse.sera.reserva.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mercadopago.resources.payment.Payment;
import edu.unse.sera.disponibilidad.control.CalendarioService;
import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.pagos.control.MercadoPagoGateway;
import edu.unse.sera.pagos.entity.MedioPago;
import edu.unse.sera.pagos.entity.Pago;
import edu.unse.sera.pagos.persistence.PagoRepository;
import edu.unse.sera.reserva.entity.EstadoReserva;
import edu.unse.sera.reserva.entity.Reserva;
import edu.unse.sera.reserva.persistence.ReservaRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class ReservaServiceTest {
  private final ReservaRepository reservas = mock(ReservaRepository.class);
  private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
  private final EspacioRepository espacios = mock(EspacioRepository.class);
  private final PagoRepository pagos = mock(PagoRepository.class);
  private final MercadoPagoGateway mp = mock(MercadoPagoGateway.class);
  private final ReservaService service =
      new ReservaService(
          reservas,
          usuarios,
          espacios,
          mock(CalendarioService.class),
          pagos,
          mp,
          mock(TransactionTemplate.class));
  private final UUID actor = UUID.randomUUID();
  private Reserva reserva;
  private Pago pago;
  private Payment remoto;

  @BeforeEach
  void preparar() {
    Usuario usuario =
        new Usuario(
            "Ana Perez",
            "ana@example.com",
            12345678,
            EstadoUsuario.ACTIVO,
            RolUsuario.USUARIO,
            "hash");
    ReflectionTestUtils.setField(usuario, "id", actor);
    Espacio espacio = new Espacio("Cancha", null, 10, BigDecimal.TEN, "Cancha", null);
    ReflectionTestUtils.setField(espacio, "id", UUID.randomUUID());
    var ahora = OffsetDateTime.now(Reserva.ZONA);
    reserva =
        new Reserva(
            usuario,
            espacio,
            ahora.toLocalDate().plusDays(1),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            1,
            BigDecimal.TEN,
            "EXTERNO",
            UUID.randomUUID(),
            ahora);
    ReflectionTestUtils.setField(reserva, "id", UUID.randomUUID());
    pago = new Pago(reserva, MedioPago.MERCADO_PAGO);
    ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
    when(reservas.findById(reserva.getId())).thenReturn(Optional.of(reserva));
    when(reservas.titular(reserva.getId())).thenReturn(Optional.of(actor));
    when(usuarios.bloquearPorId(actor)).thenReturn(Optional.of(usuario));
    when(espacios.bloquearPorId(espacio.getId())).thenReturn(Optional.of(espacio));
    when(pagos.findById(pago.getId())).thenReturn(Optional.of(pago));
    when(pagos.findByReservaId(reserva.getId())).thenReturn(Optional.of(pago));
    remoto = mock(Payment.class);
    when(remoto.getId()).thenReturn(123L);
    when(remoto.getExternalReference()).thenReturn(pago.getId().toString());
    when(remoto.getCurrencyId()).thenReturn("ARS");
    when(remoto.getTransactionAmount()).thenReturn(BigDecimal.TEN);
    when(remoto.getStatus()).thenReturn("approved");
    when(remoto.getDateApproved()).thenReturn(ahora);
    when(mp.obtenerPago(123)).thenReturn(remoto);
  }

  @Test
  void webhookConfirmaUnaVezConImporteVerificado() {
    service.recibirPago(123);
    String codigo = reserva.getCodigo();
    service.recibirPago(123);
    assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    assertThat(reserva.getCodigo()).isEqualTo(codigo);
  }

  @Test
  void rechazaMontoAjenoYNoConfirmaPendienteRemoto() {
    when(remoto.getTransactionAmount()).thenReturn(BigDecimal.ONE);
    assertThatThrownBy(() -> service.recibirPago(123)).isInstanceOf(IllegalStateException.class);
    when(remoto.getTransactionAmount()).thenReturn(BigDecimal.TEN);
    when(remoto.getStatus()).thenReturn("pending");
    service.recibirPago(123);
    assertThat(reserva.getCodigo()).isNull();
  }

  @Test
  void cobroTardioQuedaParaRevisionSinReabrirHorario() {
    service.cancelar(reserva.getId(), actor);
    service.recibirPago(123);
    assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    assertThat(reserva.getSaldoTicket()).isZero();
    assertThat(pago.isRequiereRevision()).isTrue();
  }

  @Test
  void noPermiteConsultarReservaAjena() {
    UUID otro = UUID.randomUUID();
    Usuario usuario = mock(Usuario.class);
    when(usuario.getRol()).thenReturn(RolUsuario.USUARIO);
    when(usuarios.findById(otro)).thenReturn(Optional.of(usuario));
    assertThatThrownBy(() -> service.consultar(reserva.getId(), otro))
        .isInstanceOf(OperacionNoPermitidaException.class);
  }
}
