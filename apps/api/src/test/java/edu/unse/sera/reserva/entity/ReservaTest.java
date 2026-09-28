package edu.unse.sera.reserva.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReservaTest {
  private final OffsetDateTime ahora = OffsetDateTime.parse("2026-09-28T10:00:00-03:00");
  private final Usuario usuario =
      new Usuario(
          "Ana Perez",
          "ana@example.com",
          12345678,
          EstadoUsuario.ACTIVO,
          RolUsuario.USUARIO,
          "hash");
  private final Espacio espacio =
      new Espacio("Cancha", null, 10, new BigDecimal("1000"), "Cancha", null);

  private Reserva nueva(int horas) {
    ReflectionTestUtils.setField(
        usuario, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
    Reserva r =
        new Reserva(
            usuario,
            espacio,
            ahora.toLocalDate().plusDays(1),
            LocalTime.of(12, 0),
            LocalTime.of(12 + horas, 0),
            2,
            new BigDecimal("1000"),
            "EXTERNO",
            UUID.randomUUID(),
            ahora);
    ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
    return r;
  }

  @Test
  void calculaPrecioYNoGeneraCodigoHastaConfirmar() {
    Reserva r = nueva(2);
    assertThat(r.getTotal()).isEqualByComparingTo("2000");
    assertThat(r.getCodigo()).isNull();
    r.confirmar(ahora.plusMinutes(5));
    String codigo = r.getCodigo();
    r.confirmar(ahora.plusMinutes(6));
    assertThat(r.getCodigo()).isEqualTo(codigo).startsWith("SERA-");
    assertThat(r.estadoVisible(ahora.plusDays(2))).isEqualTo("FINALIZADA");
  }

  @Test
  void ticketConservaSobranteYCubreDiferencia() {
    Reserva origen = nueva(2);
    origen.confirmar(ahora);
    origen.cancelar(ahora, false);
    Reserva destino = nueva(1);
    destino.aplicarTicket(origen);
    assertThat(destino.importeAPagar()).isZero();
    assertThat(origen.getSaldoTicket()).isEqualByComparingTo("1000");
    Reserva otra = nueva(3);
    otra.aplicarTicket(origen);
    assertThat(otra.importeAPagar()).isEqualByComparingTo("2000");
    assertThat(origen.getSaldoTicket()).isZero();
    assertThatThrownBy(() -> nueva(1).aplicarTicket(origen))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cancelacionNoDuplicaSaldoYNoAdmiteReservaIniciada() {
    Reserva r = nueva(1);
    r.confirmar(ahora);
    assertThatThrownBy(() -> r.cancelar(ahora.plusDays(2), false))
        .isInstanceOf(IllegalStateException.class);
    r.cancelar(ahora, false);
    r.cancelar(ahora, false);
    assertThat(r.getSaldoTicket()).isEqualByComparingTo("1000");
  }

  @Test
  void pendienteVencidaNoGeneraTicketNiAdmiteConfirmacion() {
    Reserva r = nueva(1);
    assertThatThrownBy(() -> r.confirmar(ahora.plusHours(1)))
        .isInstanceOf(IllegalStateException.class);
    r.cancelar(ahora.plusHours(1), true);
    assertThat(r.getSaldoTicket()).isZero();
    assertThat(r.getEstado()).isEqualTo(EstadoReserva.VENCIDA);
  }

  @Test
  void rechazaPasadoDuracionFraccionadaYExcesoDeCapacidad() {
    assertThatThrownBy(
            () ->
                Reserva.validarHorario(
                    ahora.toLocalDate(), LocalTime.of(8, 0), LocalTime.of(9, 0), ahora))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Reserva.validarHorario(
                    ahora.toLocalDate().plusDays(1),
                    LocalTime.of(8, 0),
                    LocalTime.of(8, 30),
                    ahora))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new Reserva(
                    usuario,
                    espacio,
                    ahora.toLocalDate().plusDays(1),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    11,
                    BigDecimal.TEN,
                    "EXTERNO",
                    UUID.randomUUID(),
                    ahora))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
