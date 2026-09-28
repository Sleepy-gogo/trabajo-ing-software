package edu.unse.sera.acceso.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.entity.EstadoEspacio;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.reserva.entity.EstadoReserva;
import edu.unse.sera.reserva.entity.Reserva;
import edu.unse.sera.reserva.persistence.ReservaRepository;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccesoServiceTest {
  private final ReservaRepository reservas = mock(ReservaRepository.class);
  private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
  private final MembresiaRepository membresias = mock(MembresiaRepository.class);
  private final AccesoService service = new AccesoService(reservas, usuarios, membresias);
  private static final String CODIGO = "SERA-00000000-0000-4000-8000-000000000001";
  private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-29T10:00:00-03:00");

  private Reserva reserva() {
    Reserva r = mock(Reserva.class);
    Usuario u = mock(Usuario.class);
    Espacio e = mock(Espacio.class);
    when(r.getUsuario()).thenReturn(u);
    when(r.getEspacio()).thenReturn(e);
    when(u.getEstadoCuenta()).thenReturn(EstadoUsuario.ACTIVO);
    when(u.getNombreCompleto()).thenReturn("Ana");
    when(e.getEstado()).thenReturn(EstadoEspacio.HABILITADO);
    when(e.getNombre()).thenReturn("Cancha");
    when(r.getEstado()).thenReturn(EstadoReserva.CONFIRMADA);
    when(r.getFecha()).thenReturn(LocalDate.of(2026, 9, 29));
    when(r.getDesde()).thenReturn(LocalTime.of(10, 0));
    when(r.getHasta()).thenReturn(LocalTime.of(11, 0));
    when(reservas.findByCodigo(CODIGO)).thenReturn(Optional.of(r));
    return r;
  }

  @Test
  void validaIntervaloSinConsumirReserva() {
    Reserva r = reserva();
    assertThat(service.validar(CODIGO, AHORA.minusNanos(1)).autorizado()).isFalse();
    assertThat(service.validar("  " + CODIGO + "  ", AHORA).autorizado()).isTrue();
    assertThat(service.validar(CODIGO, AHORA.plusMinutes(59)).autorizado()).isTrue();
    assertThat(service.validar(CODIGO, AHORA.plusHours(1)).autorizado()).isFalse();
    assertThat(service.validar(CODIGO, AHORA.plusDays(1)).autorizado()).isFalse();
    verify(reservas, never()).save(r);
  }

  @Test
  void rechazaCanceladasPendientesYCuentasInactivas() {
    Reserva r = reserva();
    for (EstadoReserva estado :
        new EstadoReserva[] {
          EstadoReserva.CANCELADA, EstadoReserva.PENDIENTE_PAGO, EstadoReserva.VENCIDA
        }) {
      when(r.getEstado()).thenReturn(estado);
      assertThat(service.validar(CODIGO, AHORA).autorizado()).isFalse();
    }
    when(r.getEstado()).thenReturn(EstadoReserva.CONFIRMADA);
    when(r.getUsuario().getEstadoCuenta()).thenReturn(EstadoUsuario.DESHABILITADO);
    assertThat(service.validar(CODIGO, AHORA).autorizado()).isFalse();
  }

  @Test
  void rechazaEspacioNoHabilitado() {
    Reserva r = reserva();
    when(r.getEspacio().getEstado()).thenReturn(EstadoEspacio.MANTENIMIENTO);
    assertThat(service.validar(CODIGO, AHORA).autorizado()).isFalse();
  }

  @Test
  void rechazaContenidoAjenoYCodigoInexistente() {
    assertThat(service.validar("https://example.com", AHORA).autorizado()).isFalse();
    verifyNoInteractions(reservas, usuarios, membresias);
    assertThat(service.validar(CODIGO, AHORA).tipo()).isEqualTo("DESCONOCIDO");
  }

  @Test
  void carnetUsaVigenciaRealDeMembresiaYEstadoDeCuenta() {
    Usuario u = mock(Usuario.class);
    Membresia m = mock(Membresia.class);
    UUID id = UUID.randomUUID();
    when(u.getId()).thenReturn(id);
    when(u.getEstadoCuenta()).thenReturn(EstadoUsuario.ACTIVO);
    when(usuarios.findByQrUsuario("SERA-U0123456789ab")).thenReturn(Optional.of(u));
    when(membresias.buscarMembresiaPorUsuarioId(id)).thenReturn(Optional.of(m));
    assertThat(service.validar("SERA-U0123456789ab", AHORA).autorizado()).isFalse();
    when(m.estaVigente(AHORA.toLocalDate())).thenReturn(true);
    assertThat(service.validar("SERA-U0123456789ab", AHORA).autorizado()).isTrue();
    when(u.getEstadoCuenta()).thenReturn(EstadoUsuario.INACTIVO);
    assertThat(service.validar("SERA-U0123456789ab", AHORA).autorizado()).isFalse();
  }

  @Test
  void reservaConsumidaNoAutorizaOtroIngreso() {
    Reserva r = reserva();
    when(r.getConsumidaEn()).thenReturn(AHORA);
    var resultado = service.validar(CODIGO, AHORA.plusMinutes(1));
    assertThat(resultado.autorizado()).isFalse();
    assertThat(resultado.consumidaEn()).isEqualTo(AHORA);
  }

  @Test
  void reintentarIngresoDevuelveRegistroOriginalSinVolverAConsumir() {
    Reserva r = reserva();
    when(reservas.bloquearPorCodigo(CODIGO)).thenReturn(Optional.of(r));
    when(r.getConsumidaEn()).thenReturn(AHORA);
    assertThat(service.registrarIngreso(CODIGO, UUID.randomUUID()).consumidaEn()).isEqualTo(AHORA);
    verify(r, never())
        .consumir(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
