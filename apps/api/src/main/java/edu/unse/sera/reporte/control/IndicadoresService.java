package edu.unse.sera.reporte.control;

import edu.unse.sera.actividad.control.ActividadService;
import edu.unse.sera.reporte.control.IndicadoresDetalle.Asistencia;
import edu.unse.sera.reporte.control.IndicadoresDetalle.CeldaDemanda;
import edu.unse.sera.reporte.control.IndicadoresDetalle.Indicador;
import edu.unse.sera.reporte.control.IndicadoresDetalle.IngresoMensual;
import edu.unse.sera.reporte.control.IndicadoresDetalle.MembresiasNivel;
import edu.unse.sera.reporte.control.IndicadoresDetalle.SociosRelacion;
import edu.unse.sera.reporte.control.IndicadoresDetalle.UsoEspacio;
import edu.unse.sera.reporte.persistence.ReporteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IndicadoresService {
  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final BigDecimal CIEN = BigDecimal.valueOf(100);
  private static final int MESES_MINIMOS = 6;
  private static final String[] DIAS = {
    "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"
  };

  private final ReporteRepository datos;
  private final ActividadService actividad;

  public IndicadoresService(ReporteRepository datos, ActividadService actividad) {
    this.datos = datos;
    this.actividad = actividad;
  }

  public IndicadoresDetalle consultar(LocalDate desde, LocalDate hasta) {
    return consultar(desde, hasta, LocalDateTime.now(ZONA));
  }

  IndicadoresDetalle consultar(LocalDate desde, LocalDate hasta, LocalDateTime ahora) {
    if (desde == null
        || hasta == null
        || desde.isAfter(hasta)
        || desde.plusYears(2).isBefore(hasta)) {
      throw new IllegalArgumentException("Indicá un período válido de hasta dos años.");
    }
    long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
    LocalDate anteriorHasta = desde.minusDays(1);
    LocalDate anteriorDesde = anteriorHasta.minusDays(dias - 1);
    Periodo actual = periodo(desde, hasta, ahora);
    Periodo anterior = periodo(anteriorDesde, anteriorHasta, ahora);

    LocalDate hoy = ahora.toLocalDate();
    List<MembresiasNivel> membresias = membresias(hasta.isAfter(hoy) ? hoy : hasta);
    long activas = membresias.stream().mapToLong(MembresiasNivel::cantidad).sum();
    long activasAntes =
        membresias(anteriorHasta).stream().mapToLong(MembresiasNivel::cantidad).sum();

    List<Indicador> indicadores =
        List.of(
            comparar(
                "ingresos", "Ingresos aprobados", "ARS", actual.ingresos(), anterior.ingresos()),
            comparar(
                "reservas",
                "Reservas confirmadas",
                "cantidad",
                BigDecimal.valueOf(actual.confirmadas()),
                BigDecimal.valueOf(anterior.confirmadas())),
            comparar("horas", "Horas reservadas", "horas", actual.horas(), anterior.horas()),
            new Indicador(
                "cancelacion",
                "Tasa de cancelación",
                "porcentaje",
                actual.tasaCancelacion(),
                anterior.tasaCancelacion(),
                anterior.decididas() == 0 || actual.decididas() == 0
                    ? null
                    : actual.tasaCancelacion().subtract(anterior.tasaCancelacion())),
            comparar(
                "membresias",
                "Membresías activas",
                "cantidad",
                BigDecimal.valueOf(activas),
                BigDecimal.valueOf(activasAntes)));

    List<UsoEspacio> espacios =
        datos.usoPorEspacio(desde, hasta).stream()
            .map(
                f ->
                    new UsoEspacio(
                        texto(f.get("espacio")),
                        entero(f.get("reservas")),
                        decimal(f.get("horas")).setScale(1, RoundingMode.HALF_UP),
                        decimal(f.get("ingresos"))))
            .toList();
    List<CeldaDemanda> demanda =
        datos.demanda(desde, hasta).stream()
            .map(
                f ->
                    new CeldaDemanda(
                        (int) entero(f.get("dia")),
                        (int) entero(f.get("hora")),
                        entero(f.get("reservas"))))
            .toList();
    Asistencia asistencia =
        new Asistencia(
            actual.terminadas(),
            actual.conIngreso(),
            porcentaje(actual.conIngreso(), actual.terminadas()));
    List<SociosRelacion> socios =
        datos.sociosPorRelacion().stream()
            .map(
                f ->
                    new SociosRelacion(
                        texto(f.get("relacion")),
                        entero(f.get("cantidad")),
                        entero(f.get("pendientes"))))
            .toList();

    return new IndicadoresDetalle(
        desde,
        hasta,
        anteriorDesde,
        anteriorHasta,
        indicadores,
        ingresosPorMes(desde, hasta),
        espacios,
        demanda,
        asistencia,
        socios,
        membresias,
        conclusiones(indicadores, actual, espacios, demanda, asistencia),
        actividad.recientes());
  }

  private record Periodo(
      BigDecimal ingresos,
      long confirmadas,
      long canceladas,
      BigDecimal horas,
      long terminadas,
      long conIngreso) {
    long decididas() {
      return confirmadas + canceladas;
    }

    BigDecimal tasaCancelacion() {
      return porcentaje(canceladas, decididas());
    }
  }

  private Periodo periodo(LocalDate desde, LocalDate hasta, LocalDateTime ahora) {
    Map<String, Object> f = datos.resumenPeriodo(desde, hasta, ahora).get(0);
    return new Periodo(
        decimal(f.get("ingresos")),
        entero(f.get("confirmadas")),
        entero(f.get("canceladas")),
        decimal(f.get("horas")).setScale(1, RoundingMode.HALF_UP),
        entero(f.get("terminadas")),
        entero(f.get("con_ingreso")));
  }

  private List<MembresiasNivel> membresias(LocalDate fecha) {
    return datos.membresiasActivasPorNivel(fecha).stream()
        .map(f -> new MembresiasNivel(texto(f.get("nivel")), entero(f.get("cantidad"))))
        .toList();
  }

  private List<IngresoMensual> ingresosPorMes(LocalDate desde, LocalDate hasta) {
    YearMonth ultimo = YearMonth.from(hasta);
    YearMonth primero = YearMonth.from(desde);
    if (primero.isAfter(ultimo.minusMonths(MESES_MINIMOS - 1))) {
      primero = ultimo.minusMonths(MESES_MINIMOS - 1);
    }
    Map<String, IngresoMensual> meses = new LinkedHashMap<>();
    for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
      meses.put(
          mes.toString(), new IngresoMensual(mes.toString(), BigDecimal.ZERO, BigDecimal.ZERO));
    }
    for (Map<String, Object> f : datos.ingresosPorMes(primero.atDay(1), hasta)) {
      String mes = texto(f.get("mes"));
      meses.put(mes, new IngresoMensual(mes, decimal(f.get("cuotas")), decimal(f.get("reservas"))));
    }
    return List.copyOf(meses.values());
  }

  private List<String> conclusiones(
      List<Indicador> indicadores,
      Periodo actual,
      List<UsoEspacio> espacios,
      List<CeldaDemanda> demanda,
      Asistencia asistencia) {
    List<String> textos = new ArrayList<>();
    Indicador ingresos = indicadores.get(0);
    if (ingresos.variacion() != null) {
      textos.add(tendencia("Los ingresos aprobados", ingresos.variacion()));
    }
    Indicador reservas = indicadores.get(1);
    if (reservas.variacion() != null) {
      textos.add(tendencia("Las reservas confirmadas", reservas.variacion()));
    }
    BigDecimal horas =
        espacios.stream().map(UsoEspacio::horas).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (horas.signum() > 0) {
      UsoEspacio principal =
          espacios.stream().max(Comparator.comparing(UsoEspacio::horas)).orElseThrow();
      textos.add(
          "«"
              + principal.espacio()
              + "» concentró el "
              + redondear(principal.horas().multiply(CIEN).divide(horas, 4, RoundingMode.HALF_UP))
              + "% de las horas reservadas.");
    }
    demanda.stream()
        .max(
            Comparator.comparingLong(CeldaDemanda::reservas)
                .thenComparing(CeldaDemanda::dia, Comparator.reverseOrder())
                .thenComparing(CeldaDemanda::hora, Comparator.reverseOrder()))
        .ifPresent(
            c ->
                textos.add(
                    "La franja con más demanda es "
                        + DIAS[c.dia() - 1]
                        + " de "
                        + c.hora()
                        + " a "
                        + (c.hora() + 1)
                        + "."));
    if (asistencia.terminadas() > 0) {
      long sinIngreso = asistencia.terminadas() - asistencia.conIngreso();
      textos.add(
          sinIngreso == 0
              ? "Todas las reservas que ya terminaron registraron ingreso."
              : "El "
                  + redondear(porcentaje(sinIngreso, asistencia.terminadas()))
                  + "% de las reservas que ya terminaron no registró ingreso.");
    }
    if (actual.canceladas() > 0) {
      textos.add(
          "Se canceló el "
              + redondear(actual.tasaCancelacion())
              + "% de las reservas del período.");
    }
    return textos;
  }

  private static String tendencia(String sujeto, BigDecimal variacion) {
    int signo = variacion.signum();
    if (signo == 0) {
      return sujeto + " se mantuvieron iguales que en el período anterior.";
    }
    return sujeto
        + (signo > 0 ? " crecieron un " : " bajaron un ")
        + redondear(variacion.abs())
        + "% respecto del período anterior.";
  }

  private static Indicador comparar(
      String clave, String nombre, String unidad, BigDecimal valor, BigDecimal anterior) {
    BigDecimal variacion =
        anterior.signum() == 0
            ? null
            : valor.subtract(anterior).multiply(CIEN).divide(anterior, 1, RoundingMode.HALF_UP);
    return new Indicador(clave, nombre, unidad, valor, anterior, variacion);
  }

  private static BigDecimal porcentaje(long parte, long total) {
    return total == 0
        ? BigDecimal.ZERO.setScale(1)
        : BigDecimal.valueOf(parte)
            .multiply(CIEN)
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
  }

  private static String redondear(BigDecimal valor) {
    return valor.setScale(0, RoundingMode.HALF_UP).toPlainString();
  }

  private static BigDecimal decimal(Object valor) {
    return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
  }

  private static long entero(Object valor) {
    return valor == null ? 0 : ((Number) valor).longValue();
  }

  private static String texto(Object valor) {
    return valor == null ? "" : valor.toString();
  }
}
