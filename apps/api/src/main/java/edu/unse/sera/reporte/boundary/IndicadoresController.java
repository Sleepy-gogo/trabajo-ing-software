package edu.unse.sera.reporte.boundary;

import edu.unse.sera.reporte.control.IndicadoresDetalle;
import edu.unse.sera.reporte.control.IndicadoresService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Panel de indicadores de administración. La configuración de seguridad lo limita a ADMIN. */
@RestController
@RequestMapping("/api/reportes/indicadores")
public class IndicadoresController {
  private final IndicadoresService indicadores;

  public IndicadoresController(IndicadoresService indicadores) {
    this.indicadores = indicadores;
  }

  @GetMapping
  public IndicadoresDetalle consultar(
      @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
    return indicadores.consultar(desde, hasta);
  }
}
