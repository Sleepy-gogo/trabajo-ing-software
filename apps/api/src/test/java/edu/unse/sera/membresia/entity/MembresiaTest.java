package edu.unse.sera.membresia.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MembresiaTest {
  private NivelMembresia nivel() {
    var n = new NivelMembresia("General", "Acceso general");
    n.actualizar(
        "General",
        "Acceso general",
        Map.of(RelacionUnse.EXTERNO, new BigDecimal("1000.00")),
        List.of("Acceso"),
        true);
    return n;
  }

  @Test
  void comienzaPendienteSinVencimientoDeCuota() {
    var m = new Membresia(null, nivel());
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
    assertThat(m.getFechaAlta()).isNotNull();
    assertThat(m.getProximoVencimiento()).isNull();
  }

  @Test
  void noPermiteActivacionAdministrativaSinPago() {
    var m = new Membresia(null, nivel());
    assertThatThrownBy(() -> m.cambiarEstado(EstadoMembresia.ACTIVA))
        .isInstanceOf(IllegalStateException.class);
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
  }

  @Test
  void cancelaInmediatamenteYReutilizaLaMismaMembresia() {
    var m = new Membresia(null, nivel());
    var contratacionAnterior = m.getContratacionId();
    m.cancelar();
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.CANCELADA);
    assertThat(m.getFechaBaja()).isNotNull();
    m.renovarSolicitud(nivel());
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.PENDIENTE_PAGO);
    assertThat(m.getFechaBaja()).isNull();
    assertThat(m.getContratacionId()).isNotEqualTo(contratacionAnterior);
  }

  @Test
  void rechazaContratarSobreSolicitudPendiente() {
    var m = new Membresia(null, nivel());
    assertThatThrownBy(() -> m.renovarSolicitud(nivel())).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaPrecioNegativoYMasDeDosDecimales() {
    var n = nivel();
    for (var importe : List.of("-1", "0", "1.001")) {
      assertThatThrownBy(
              () ->
                  n.actualizar(
                      "General",
                      "Descripción",
                      Map.of(RelacionUnse.EXTERNO, new BigDecimal(importe)),
                      List.of("Acceso"),
                      true))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(n.getPreciosPorRelacion())
        .containsEntry(RelacionUnse.EXTERNO, new BigDecimal("1000.00"));
  }

  @Test
  void venceDespuesDelDiaDePagoYPierdeBeneficios() {
    var m = new Membresia(null, nivel());
    m.activarPorPago();
    LocalDate vencimiento = LocalDate.of(2026, 1, 31);
    m.setProximoVencimiento(vencimiento);

    assertThat(m.estaVigente(vencimiento.minusDays(1))).isTrue();
    assertThat(m.estaVigente(vencimiento)).isFalse();
    m.actualizarPorVencimiento(vencimiento);
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.VENCIDA);

    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.VENCIDA);
    assertThat(m.tieneBeneficios()).isFalse();

    m.actualizarPorVencimiento(vencimiento.plusMonths(2).minusDays(1));
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.VENCIDA);
    m.actualizarPorVencimiento(vencimiento.plusMonths(2));
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.VENCIDA);
  }

  @Test
  void recuperaEstadoCorrectoSiElProcesoDiarioNoCorrio() {
    var m = new Membresia(null, nivel());
    m.activarPorPago();
    m.setProximoVencimiento(LocalDate.of(2026, 1, 31));

    m.actualizarPorVencimiento(LocalDate.of(2026, 4, 1));

    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.VENCIDA);
    assertThat(m.tieneBeneficios()).isFalse();
  }

  @Test
  void renovacionAnticipadaSumaDesdeVencimientoYLaAtrasadaDesdePago() {
    var m = new Membresia(null, nivel());
    assertThat(m.renovarUnMes(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 31)))
        .isEqualTo(LocalDate.of(2026, 2, 28));
    assertThat(m.renovarUnMes(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10)))
        .isEqualTo(LocalDate.of(2026, 3, 28));
    assertThat(m.renovarUnMes(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 5)))
        .isEqualTo(LocalDate.of(2026, 5, 5));
  }

  @Test
  void suspensionAdministrativaImpideRenovarYConservaElVencimiento() {
    var m = new Membresia(null, nivel());
    var vencimiento = m.renovarUnMes(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 27));
    m.cambiarEstado(EstadoMembresia.SUSPENDIDA);

    assertThat(m.admitePago()).isFalse();
    assertThatThrownBy(() -> m.renovarUnMes(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(m.getEstado()).isEqualTo(EstadoMembresia.SUSPENDIDA);
    assertThat(m.getProximoVencimiento()).isEqualTo(vencimiento);
  }
}
