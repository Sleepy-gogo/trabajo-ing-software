package edu.unse.sera.reporte.control;

import edu.unse.sera.actividad.control.ActividadDetalle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

  public record Indicador(
      String clave,
      String nombre,
      String unidad,
      BigDecimal valor,
      BigDecimal anterior,
      BigDecimal variacion) {}

  public record IngresoMensual(String mes, BigDecimal cuotas, BigDecimal reservas) {}

  public record UsoEspacio(String espacio, long reservas, BigDecimal horas, BigDecimal ingresos) {}

  public record CeldaDemanda(int dia, int hora, long reservas) {}

  public record Asistencia(long terminadas, long conIngreso, BigDecimal porcentaje) {}

  public record SociosRelacion(String relacion, long cantidad, long pendientes) {}

  public record MembresiasNivel(String nivel, long cantidad) {}
}
