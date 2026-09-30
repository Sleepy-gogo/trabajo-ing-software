package edu.unse.sera.reporte.boundary;

import edu.unse.sera.reporte.control.ReporteDetalle;
import edu.unse.sera.reporte.control.ReporteFiltros;
import edu.unse.sera.reporte.control.ReporteService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
  private final ReporteService reportes;

  public ReporteController(ReporteService reportes) {
    this.reportes = reportes;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReporteDetalle generar(@RequestBody ReporteFiltros filtros, Principal actor) {
    return reportes.generar(filtros, UUID.fromString(actor.getName()));
  }

  @GetMapping
  public List<ReporteDetalle> recientes(@RequestParam(defaultValue = "0") int pagina) {
    return reportes.recientes(pagina);
  }

  @GetMapping("/{id}")
  public ReporteDetalle consultar(@PathVariable UUID id) {
    return reportes.consultar(id);
  }

  @GetMapping(value = "/{id}/csv", produces = "text/csv;charset=UTF-8")
  public ResponseEntity<String> csv(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sera-" + id + ".csv")
        .body(reportes.csv(id));
  }
}
