package edu.unse.sera.reporte.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.unse.sera.actividad.control.ActividadService;
import edu.unse.sera.actividad.entity.TipoActividad;
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
    var actividad = mock(ActividadService.class);
    var service =
        new ReporteService(
            datos,
            informes,
            usuarios,
            mapper,
            mock(edu.unse.sera.espacio.persistence.EspacioRepository.class),
            actividad);
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
    org.mockito.Mockito.verify(actividad)
        .registrar(
            eq(actor),
            any(),
            eq(TipoActividad.INFORME_GENERADO),
            org.mockito.ArgumentMatchers.startsWith("Informe de pagos del "));
  }

  @Test
  void elInformeDeActividadFiltraPorTipoYExportaLasMismasFilas() {
    var datos = mock(ReporteRepository.class);
    var informes = mock(InformeRepository.class);
    var usuarios = mock(UsuarioRepository.class);
    var actividad = mock(ActividadService.class);
    var service =
        new ReporteService(
            datos,
            informes,
            usuarios,
            JsonMapper.builder().findAndAddModules().build(),
            mock(edu.unse.sera.espacio.persistence.EspacioRepository.class),
            actividad);
    UUID actor = UUID.randomUUID();
    var usuario = mock(Usuario.class);
    when(usuario.getEmail()).thenReturn("admin@example.com");
    when(usuarios.findById(actor)).thenReturn(Optional.of(usuario));
    var desde = LocalDate.of(2026, 11, 1);
    var hasta = LocalDate.of(2026, 11, 17);
    var filtros = new ReporteFiltros("actividad", desde, hasta, "INICIO_SESION_FALLIDO", "", null);
    when(datos.actividad(desde, hasta, "INICIO_SESION_FALLIDO"))
        .thenReturn(
            List.of(
                Map.of(
                    "fecha", "2026-11-17 10:00:00",
                    "tipo", "INICIO_SESION_FALLIDO",
                    "accion", "Inicio de sesión fallido",
                    "usuario", "ana@example.com",
                    "nombre", "Ana",
                    "detalle", "Contraseña incorrecta")));

    var reporte = service.generar(filtros, actor);

    assertThat(reporte.columnas())
        .extracting(ReporteDetalle.Columna::key)
        .containsExactly("fecha", "accion", "usuario", "nombre", "detalle");
    var guardado = ArgumentCaptor.forClass(Informe.class);
    org.mockito.Mockito.verify(informes).save(guardado.capture());
    when(informes.findById(reporte.id())).thenReturn(Optional.of(guardado.getValue()));
    assertThat(service.csv(reporte.id()))
        .contains(
            "\"Fecha y hora\",\"Acción\",\"Usuario\",\"Nombre\",\"Detalle\"",
            "\"2026-11-17 10:00:00\",\"Inicio de sesión fallido\",\"ana@example.com\",\"Ana\","
                + "\"Contraseña incorrecta\"");
    org.mockito.Mockito.verify(actividad)
        .registrar(
            actor,
            "admin@example.com",
            TipoActividad.INFORME_GENERADO,
            "Informe de actividad del 01/11/2026 al 17/11/2026");
  }

  @Test
  void elInformeDeActividadSoloAceptaTiposDeAccionYNoFiltraPorRelacionNiEspacio() {
    var hoy = LocalDate.of(2026, 11, 17);
    assertThat(new ReporteFiltros("actividad", hoy, hoy, null, null, null).estado()).isEmpty();
    assertThatThrownBy(() -> new ReporteFiltros("actividad", hoy, hoy, "APROBADO", "", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ReporteFiltros("actividad", hoy, hoy, "", "EXTERNO", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ReporteFiltros("actividad", hoy, hoy, "", "", UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
