package edu.unse.sera.reporte.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.unse.sera.actividad.control.ActividadDetalle;
import edu.unse.sera.actividad.control.ActividadService;
import edu.unse.sera.actividad.entity.TipoActividad;
import edu.unse.sera.reporte.control.IndicadoresDetalle.Indicador;
import edu.unse.sera.reporte.control.IndicadoresDetalle.IngresoMensual;
import edu.unse.sera.reporte.persistence.ReporteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndicadoresServiceTest {
  private final ReporteRepository datos = mock(ReporteRepository.class);
  private final ActividadService actividad = mock(ActividadService.class);
  private final IndicadoresService service = new IndicadoresService(datos, actividad);
  private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 17, 12, 0);
  private final LocalDate desde = LocalDate.of(2026, 10, 19);
  private final LocalDate hasta = LocalDate.of(2026, 11, 17);
  private final LocalDate anteriorDesde = LocalDate.of(2026, 9, 19);
  private final LocalDate anteriorHasta = LocalDate.of(2026, 10, 18);

  @BeforeEach
  void preparar() {
    when(datos.resumenPeriodo(any(), any(), any()))
        .thenReturn(List.of(resumen("0", 0, 0, "0", 0, 0)));
    when(datos.membresiasActivasPorNivel(any())).thenReturn(List.of());
  }

  @Test
  void comparaConElPeriodoAnteriorDeIgualDuracion() {
    when(datos.resumenPeriodo(desde, hasta, ahora))
        .thenReturn(List.of(resumen("1180.00", 9, 1, "12.0", 0, 0)));
    when(datos.resumenPeriodo(anteriorDesde, anteriorHasta, ahora))
        .thenReturn(List.of(resumen("1000.00", 6, 2, "8.0", 0, 0)));
    when(datos.membresiasActivasPorNivel(hasta))
        .thenReturn(List.of(Map.of("nivel", "General", "cantidad", 3L)));
    when(datos.membresiasActivasPorNivel(anteriorHasta))
        .thenReturn(List.of(Map.of("nivel", "General", "cantidad", 4L)));

    var panel = service.consultar(desde, hasta, ahora);

    assertThat(panel.anteriorDesde()).isEqualTo(anteriorDesde);
    assertThat(panel.anteriorHasta()).isEqualTo(anteriorHasta);
    assertThat(panel.indicadores())
        .extracting(Indicador::clave, Indicador::valor, Indicador::anterior, Indicador::variacion)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                "ingresos",
                new BigDecimal("1180.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("18.0")),
            org.assertj.core.groups.Tuple.tuple(
                "reservas", BigDecimal.valueOf(9), BigDecimal.valueOf(6), new BigDecimal("50.0")),
            org.assertj.core.groups.Tuple.tuple(
                "horas", new BigDecimal("12.0"), new BigDecimal("8.0"), new BigDecimal("50.0")),
            org.assertj.core.groups.Tuple.tuple(
                "cancelacion",
                new BigDecimal("10.0"),
                new BigDecimal("25.0"),
                new BigDecimal("-15.0")),
            org.assertj.core.groups.Tuple.tuple(
                "membresias",
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(4),
                new BigDecimal("-25.0")));
    assertThat(panel.conclusiones())
        .contains(
            "Los ingresos aprobados crecieron un 18% respecto del período anterior.",
            "Las reservas confirmadas crecieron un 50% respecto del período anterior.",
            "Se canceló el 10% de las reservas del período.");
  }

  @Test
  void sinIngresosEnElPeriodoAnteriorNoInformaUnPorcentajeDeCrecimiento() {
    when(datos.resumenPeriodo(desde, hasta, ahora))
        .thenReturn(List.of(resumen("5000.00", 0, 0, "0", 0, 0)));

    var panel = service.consultar(desde, hasta, ahora);

    assertThat(panel.indicadores().get(0).variacion()).isNull();
    assertThat(panel.indicadores().get(3).variacion()).isNull();
    assertThat(panel.conclusiones()).noneMatch(c -> c.contains("crecieron") || c.contains("%"));
  }

  @Test
  void separaCuotasDeReservasYCompletaLosMesesSinIngresos() {
    when(datos.ingresosPorMes(LocalDate.of(2026, 6, 1), hasta))
        .thenReturn(
            List.of(
                Map.of(
                    "mes",
                    "2026-09",
                    "cuotas",
                    new BigDecimal("3000.00"),
                    "reservas",
                    BigDecimal.ZERO),
                Map.of(
                    "mes", "2026-11",
                    "cuotas", new BigDecimal("1500.00"),
                    "reservas", new BigDecimal("2400.00"))));

    var meses = service.consultar(desde, hasta, ahora).ingresosPorMes();

    assertThat(meses)
        .extracting(IngresoMensual::mes)
        .containsExactly("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11");
    assertThat(meses.get(3).cuotas()).isEqualByComparingTo("3000");
    assertThat(meses.get(4).cuotas()).isZero();
    assertThat(meses.get(5).reservas()).isEqualByComparingTo("2400");
  }

  @Test
  void unPeriodoLargoMuestraTodosSusMeses() {
    var meses = service.consultar(LocalDate.of(2025, 11, 18), hasta, ahora).ingresosPorMes();
    assertThat(meses).hasSize(13);
    assertThat(meses.get(0).mes()).isEqualTo("2025-11");
  }

  @Test
  void escribeConclusionesSobreEspaciosDemandaYAsistencia() {
    when(datos.resumenPeriodo(desde, hasta, ahora))
        .thenReturn(List.of(resumen("0", 25, 0, "25.0", 10, 7)));
    when(datos.usoPorEspacio(desde, hasta))
        .thenReturn(
            List.of(
                Map.of(
                    "espacio",
                    "Cancha cubierta",
                    "reservas",
                    10L,
                    "horas",
                    new BigDecimal("13"),
                    "ingresos",
                    BigDecimal.TEN),
                Map.of(
                    "espacio",
                    "Pileta",
                    "reservas",
                    12L,
                    "horas",
                    new BigDecimal("12"),
                    "ingresos",
                    BigDecimal.ONE)));
    when(datos.demanda(desde, hasta))
        .thenReturn(
            List.of(
                Map.of("dia", 2, "hora", 18, "reservas", 3L),
                Map.of("dia", 4, "hora", 18, "reservas", 5L),
                Map.of("dia", 4, "hora", 19, "reservas", 5L)));
    when(datos.sociosPorRelacion())
        .thenReturn(List.of(Map.of("relacion", "ESTUDIANTE", "cantidad", 4L, "pendientes", 1L)));
    var accion =
        new ActividadDetalle(
            TipoActividad.INICIO_SESION, "ana@example.com", null, OffsetDateTime.now());
    when(actividad.recientes()).thenReturn(List.of(accion));

    var panel = service.consultar(desde, hasta, ahora);

    assertThat(panel.asistencia().porcentaje()).isEqualByComparingTo("70");
    assertThat(panel.espacios().get(0).horas()).isEqualByComparingTo("13.0");
    assertThat(panel.sociosPorRelacion().get(0).pendientes()).isEqualTo(1);
    assertThat(panel.ultimasAcciones()).containsExactly(accion);
    assertThat(panel.conclusiones())
        .containsExactly(
            "«Cancha cubierta» concentró el 52% de las horas reservadas.",
            "La franja con más demanda es jueves de 18 a 19.",
            "El 30% de las reservas que ya terminaron no registró ingreso.");
  }

  @Test
  void informaCuandoTodasLasReservasTerminadasRegistraronIngresoYCuandoNoHayCambios() {
    when(datos.resumenPeriodo(desde, hasta, ahora))
        .thenReturn(List.of(resumen("100", 2, 0, "2", 2, 2)));
    when(datos.resumenPeriodo(anteriorDesde, anteriorHasta, ahora))
        .thenReturn(List.of(resumen("100", 4, 0, "4", 0, 0)));

    assertThat(service.consultar(desde, hasta, ahora).conclusiones())
        .containsExactly(
            "Los ingresos aprobados se mantuvieron iguales que en el período anterior.",
            "Las reservas confirmadas bajaron un 50% respecto del período anterior.",
            "Todas las reservas que ya terminaron registraron ingreso.");
  }

  @Test
  void lasMembresiasDeUnPeriodoQueTerminaEnElFuturoSeCuentanHoy() {
    service.consultar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), ahora);
    org.mockito.Mockito.verify(datos).membresiasActivasPorNivel(ahora.toLocalDate());
    org.mockito.Mockito.verify(datos).membresiasActivasPorNivel(LocalDate.of(2026, 10, 31));
  }

  @Test
  void rechazaPeriodosInvalidos() {
    assertThatThrownBy(() -> service.consultar(hasta, desde, ahora))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.consultar(null, hasta, ahora))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.consultar(desde, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.consultar(hasta.minusYears(3), hasta, ahora))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Map<String, Object> resumen(
      String ingresos,
      long confirmadas,
      long canceladas,
      String horas,
      long terminadas,
      long conIngreso) {
    return Map.of(
        "ingresos", new BigDecimal(ingresos),
        "confirmadas", confirmadas,
        "canceladas", canceladas,
        "horas", new BigDecimal(horas),
        "terminadas", terminadas,
        "con_ingreso", conIngreso);
  }
}
