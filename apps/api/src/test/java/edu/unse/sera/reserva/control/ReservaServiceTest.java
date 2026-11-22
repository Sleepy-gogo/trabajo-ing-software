package edu.unse.sera.reserva.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadopago.resources.payment.Payment;
import edu.unse.sera.actividad.control.ActividadService;
import edu.unse.sera.actividad.entity.TipoActividad;
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
  private final CalendarioService calendario = mock(CalendarioService.class);
  private final ActividadService actividad = mock(ActividadService.class);
  private final ReservaService service =
      new ReservaService(
          reservas,
          usuarios,
          espacios,
          calendario,
          pagos,
          mp,
          mock(TransactionTemplate.class),
          actividad);
  private final UUID actor = UUID.randomUUID();
  private Reserva reserva;
  private Pago pago;
  private Payment remoto;
  private Usuario titular;
  private Espacio espacio;

  @BeforeEach
  void preparar() {
    titular =
        new Usuario(
            "Ana Perez",
            "ana@example.com",
            12345678,
            EstadoUsuario.ACTIVO,
            RolUsuario.USUARIO,
            "hash");
    Usuario usuario = titular;
    ReflectionTestUtils.setField(usuario, "id", actor);
    espacio = new Espacio("Cancha", null, 10, BigDecimal.TEN, "Cancha", null);
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

  @Test
  void registraLaCreacionYLaCancelacionConElEspacioYElHorario() {
    var fecha = reserva.getFecha().plusDays(1);
    var solicitud =
        new ReservaService.Solicitud(
            espacio.getId(),
            fecha,
            LocalTime.of(18, 0),
            LocalTime.of(20, 0),
            2,
            null,
            MedioPago.EFECTIVO,
            UUID.randomUUID());
    when(reservas.findByUsuarioIdAndClaveSolicitud(actor, solicitud.claveSolicitud()))
        .thenReturn(Optional.empty());
    when(espacios.findById(espacio.getId())).thenReturn(Optional.of(espacio));
    when(calendario.consultar(espacio.getId(), fecha, actor))
        .thenReturn(
            new CalendarioService.Calendario(
                fecha,
                espacio.getEstado(),
                BigDecimal.TEN,
                edu.unse.sera.socio.entity.RelacionUnse.EXTERNO,
                java.util.List.of(
                    new CalendarioService.Franja(LocalTime.of(8, 0), LocalTime.of(22, 0)))));
    when(reservas.saveAndFlush(any(Reserva.class)))
        .thenAnswer(
            invocation -> {
              Reserva nueva = invocation.getArgument(0);
              ReflectionTestUtils.setField(nueva, "id", UUID.randomUUID());
              return nueva;
            });

    service.crear(actor, solicitud);
    service.cancelar(reserva.getId(), actor);
    service.cancelar(reserva.getId(), actor);

    String horario =
        fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            + " de 18:00 a 20:00";
    verify(actividad)
        .registrar(actor, "ana@example.com", TipoActividad.RESERVA_CREADA, "Cancha, " + horario);
    verify(actividad)
        .registrar(
            actor,
            "ana@example.com",
            TipoActividad.RESERVA_CANCELADA,
            "Cancha, "
                + reserva
                    .getFecha()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " de 10:00 a 11:00");
    verify(actividad, org.mockito.Mockito.times(2)).registrar(any(), any(), any(), any());
  }

  @Test
  void laCancelacionPorAdministracionRegistraAlAdministradorYAlTitular() {
    UUID admin = UUID.randomUUID();
    Usuario administrador =
        new Usuario(
            "Admin Prueba",
            "admin@example.com",
            23456789,
            EstadoUsuario.ACTIVO,
            RolUsuario.ADMIN,
            "hash");
    ReflectionTestUtils.setField(administrador, "id", admin);
    when(usuarios.findById(admin)).thenReturn(Optional.of(administrador));

    service.cancelar(reserva.getId(), admin);

    verify(actividad)
        .registrar(
            org.mockito.ArgumentMatchers.eq(admin),
            org.mockito.ArgumentMatchers.eq("admin@example.com"),
            org.mockito.ArgumentMatchers.eq(TipoActividad.RESERVA_CANCELADA),
            org.mockito.ArgumentMatchers.endsWith(", reserva de Ana Perez"));
  }

  @Test
  void unaSolicitudRepetidaNoRegistraOtraReserva() {
    var solicitud =
        new ReservaService.Solicitud(
            reserva.getEspacio().getId(),
            reserva.getFecha(),
            reserva.getDesde(),
            reserva.getHasta(),
            reserva.getPersonas(),
            null,
            MedioPago.MERCADO_PAGO,
            reserva.getClaveSolicitud());
    when(reservas.findByUsuarioIdAndClaveSolicitud(actor, reserva.getClaveSolicitud()))
        .thenReturn(Optional.of(reserva));

    service.crear(actor, solicitud);

    verify(actividad, never()).registrar(any(), any(), any(), any());
  }
}
