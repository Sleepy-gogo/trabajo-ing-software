package edu.unse.sera.reporte.control;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ReporteFiltros(
    String tipo, LocalDate desde, LocalDate hasta, String estado, String relacion, UUID espacioId) {
  public ReporteFiltros {
    if (tipo == null || !Set.of("socios", "reservas", "pagos", "uso_servicios").contains(tipo)) {
      throw new IllegalArgumentException("Elegí un tipo de informe válido.");
    }
    if (desde == null
        || hasta == null
        || desde.isAfter(hasta)
        || desde.plusYears(5).isBefore(hasta)) {
      throw new IllegalArgumentException("Indicá un período válido de hasta cinco años.");
    }
    estado = estado == null ? "" : estado;
    relacion = relacion == null ? "" : relacion;
    Set<String> estados =
        switch (tipo) {
          case "socios" ->
              Set.of(
                  "ACTIVA",
                  "VENCIDA",
                  "PENDIENTE_PAGO",
                  "SUSPENDIDA",
                  "CANCELADA",
                  "SIN_MEMBRESIA",
                  "DESHABILITADO",
                  "INACTIVO");
          case "reservas" ->
              Set.of(
                  "PENDIENTE_PAGO",
                  "CONFIRMADA",
                  "CANCELADA",
                  "VENCIDA",
                  "FINALIZADA",
                  "EN_CURSO",
                  "CONSUMIDA");
          case "pagos" -> Set.of("PENDIENTE", "APROBADO", "RECHAZADO", "CANCELADO");
          default -> Set.of();
        };
    if (!estado.isEmpty() && !estados.contains(estado)) {
      throw new IllegalArgumentException("El estado no corresponde al tipo de informe.");
    }
    if (!relacion.isEmpty()) {
      edu.unse.sera.socio.entity.RelacionUnse.valueOf(relacion);
    }
    if (tipo.equals("socios") && espacioId != null) {
      throw new IllegalArgumentException("El informe de socios no filtra por espacio.");
    }
  }
}
