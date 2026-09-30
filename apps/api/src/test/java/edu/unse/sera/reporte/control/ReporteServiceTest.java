package edu.unse.sera.reporte.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.unse.sera.reporte.entity.Informe;
import edu.unse.sera.reporte.persistence.InformeRepository;
import edu.unse.sera.reporte.persistence.ReporteRepository;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ReporteServiceTest {
  @Test
  void validaFiltrosEnLugarDeIgnorarlos() {
    var hoy = LocalDate.now();
    assertThatThrownBy(() -> new ReporteFiltros("pagos", hoy.plusDays(1), hoy, "", "", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ReporteFiltros("pagos", hoy, hoy, "ACTIVA", "", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ReporteFiltros("socios", hoy, hoy, "", "", UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void csvEscapaComillasSaltosDeLineaYFormulas() {
    assertThat(ReporteService.celdaCsv("A,\"B\"\nC")).isEqualTo("\"A,\"\"B\"\"\nC\"");
    assertThat(ReporteService.celdaCsv(" =SUM(1,2)\nX")).startsWith("\"' =SUM");
    assertThat(ReporteService.celdaCsv(new BigDecimal("-12.50"))).isEqualTo("\"-12.50\"");
  }

  @Test
  void guardaInstantaneaConFiltrosYSoloSumaAprobados() {
    var datos = mock(ReporteRepository.class);
    var informes = mock(InformeRepository.class);
    var usuarios = mock(UsuarioRepository.class);
    var mapper = JsonMapper.builder().findAndAddModules().build();
    var service =
        new ReporteService(
            datos,
            informes,
            usuarios,
            mapper,
            mock(edu.unse.sera.espacio.persistence.EspacioRepository.class));
    UUID actor = UUID.randomUUID();
    var usuario = mock(Usuario.class);
    when(usuario.getNombreCompleto()).thenReturn("Admin");
    when(usuarios.findById(actor)).thenReturn(Optional.of(usuario));
    var filtros = new ReporteFiltros("pagos", LocalDate.now(), LocalDate.now(), "", "", null);
    when(datos.pagos(eq(filtros.desde()), eq(filtros.hasta()), eq(""), eq(""), any()))
        .thenReturn(
            List.of(
                Map.of("estado", "APROBADO", "monto", new BigDecimal("10.50")),
                Map.of("estado", "PENDIENTE", "monto", new BigDecimal("99.00"))));
    var reporte = service.generar(filtros, actor);
    assertThat(reporte.resumen()).containsEntry("importe_aprobado", new BigDecimal("10.50"));
    var guardado = ArgumentCaptor.forClass(Informe.class);
    org.mockito.Mockito.verify(informes).save(guardado.capture());
    when(informes.findById(reporte.id())).thenReturn(Optional.of(guardado.getValue()));
    assertThat(service.consultar(reporte.id()).filtros()).isEqualTo(filtros);
    assertThat(service.csv(reporte.id())).contains("\"10.50\"", "\"99.00\"");
  }
}
