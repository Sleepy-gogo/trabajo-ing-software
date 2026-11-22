package edu.unse.sera.reporte.boundary;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.reporte.control.IndicadoresDetalle;
import edu.unse.sera.reporte.control.IndicadoresService;
import edu.unse.sera.shared.config.SecurityConfig;
import edu.unse.sera.usuario.control.UsuarioService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IndicadoresController.class)
@Import(SecurityConfig.class)
class IndicadoresControllerTest {
  private static final String ACTOR = "11111111-1111-4111-8111-111111111111";
  private static final String RUTA = "/api/reportes/indicadores?desde=2026-11-01&hasta=2026-11-17";
  private final MockMvc mvc;
  @MockitoBean private IndicadoresService indicadores;
  @MockitoBean private UsuarioService usuarios;

  @Autowired
  IndicadoresControllerTest(MockMvc mvc) {
    this.mvc = mvc;
  }

  @Test
  void soloAdministracionPuedeVerElPanel() throws Exception {
    mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    for (String rol : new String[] {"USUARIO", "STAFF"}) {
      mvc.perform(get(RUTA).with(user(ACTOR).roles(rol))).andExpect(status().isForbidden());
    }
    verifyNoInteractions(indicadores);
  }

  @Test
  void administracionConsultaElPanelDelPeriodo() throws Exception {
    var desde = LocalDate.of(2026, 11, 1);
    var hasta = LocalDate.of(2026, 11, 17);
    when(indicadores.consultar(desde, hasta))
        .thenReturn(
            new IndicadoresDetalle(
                desde,
                hasta,
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 31),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new IndicadoresDetalle.Asistencia(0, 0, java.math.BigDecimal.ZERO),
                List.of(),
                List.of(),
                List.of("Sin datos"),
                List.of()));

    mvc.perform(get(RUTA).with(user(ACTOR).roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anteriorDesde").value("2026-10-15"))
        .andExpect(jsonPath("$.conclusiones[0]").value("Sin datos"));
  }

  @Test
  void rechazaUnPeriodoSinFechas() throws Exception {
    mvc.perform(get("/api/reportes/indicadores").with(user(ACTOR).roles("ADMIN")))
        .andExpect(status().isBadRequest());
  }
}
