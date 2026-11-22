package edu.unse.sera.reporte.control;

import edu.unse.sera.actividad.control.ActividadDetalle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Panel de indicadores de un período, comparado con el período anterior de igual duración. */
public record IndicadoresDetalle(
    LocalDate desde,
    LocalDate hasta,
    LocalDate anteriorDesde,
    LocalDate anteriorHasta,
    List<Indicador> indicadores,
    List<IngresoMensual> ingresosPorMes,
    List<UsoEspacio> espacios,
    List<CeldaDemanda> demanda,
    Asistencia asistencia,
    List<SociosRelacion> sociosPorRelacion,
    List<MembresiasNivel> membresiasPorNivel,
    List<String> conclusiones,
    List<ActividadDetalle> ultimasAcciones) {

  /**
   * Valor del período y del anterior. La variación es porcentual, salvo para la tasa de
   * cancelación, donde es la diferencia en puntos. Es {@code null} si no hay base de comparación.
   */
  public record Indicador(
      String clave,
      String nombre,
      String unidad,
      BigDecimal valor,
      BigDecimal anterior,
      BigDecimal variacion) {}

  public record IngresoMensual(String mes, BigDecimal cuotas, BigDecimal reservas) {}

  public record UsoEspacio(String espacio, long reservas, BigDecimal horas, BigDecimal ingresos) {}

  /** Día ISO (1 = lunes) y hora de inicio de la franja de una hora. */
  public record CeldaDemanda(int dia, int hora, long reservas) {}

  public record Asistencia(long terminadas, long conIngreso, BigDecimal porcentaje) {}

  public record SociosRelacion(String relacion, long cantidad, long pendientes) {}

  public record MembresiasNivel(String nivel, long cantidad) {}
}
