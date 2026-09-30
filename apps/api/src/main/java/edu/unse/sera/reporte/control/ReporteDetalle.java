package edu.unse.sera.reporte.control;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReporteDetalle(
    UUID id,
    String tipo,
    ReporteFiltros filtros,
    OffsetDateTime creadoEn,
    String creadoPor,
    String espacioNombre,
    List<Columna> columnas,
    List<Map<String, Object>> filas,
    Map<String, Object> resumen) {
  public record Columna(String key, String label) {}
}
