package edu.unse.sera.reporte.boundary;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.encuesta.boundary.EncuestaController;
import edu.unse.sera.encuesta.control.EncuestaService;
import edu.unse.sera.reporte.control.ReporteService;
import edu.unse.sera.shared.config.SecurityConfig;
import edu.unse.sera.usuario.control.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ReporteController.class, EncuestaController.class})
@Import(SecurityConfig.class)
class CierreControllerTest {
  private final MockMvc mvc;
  @MockitoBean private ReporteService reportes;
  @MockitoBean private EncuestaService encuestas;
  @MockitoBean private UsuarioService usuarios;
  private static final String ACTOR = "11111111-1111-4111-8111-111111111111";

  @Autowired
  CierreControllerTest(MockMvc mvc) {
    this.mvc = mvc;
  }

  @Test
  void informesYResultadosSoloAdministracion() throws Exception {
    for (String ruta :
        new String[] {
          "/api/reportes",
          "/api/reportes/" + ACTOR + "/csv",
          "/api/encuestas",
          "/api/encuestas/" + ACTOR + "/resultados"
        }) {
      mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
      for (String rol : new String[] {"USUARIO", "STAFF"}) {
        mvc.perform(get(ruta).with(user(ACTOR).roles(rol))).andExpect(status().isForbidden());
      }
    }
    verifyNoInteractions(reportes, encuestas);
    mvc.perform(get("/api/reportes").with(user(ACTOR).roles("ADMIN"))).andExpect(status().isOk());
  }

  @Test
  void encuestasPropiasRequierenSesionYEnvioRequiereCsrf() throws Exception {
    mvc.perform(get("/api/encuestas/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/encuestas/me").with(user(ACTOR).roles("USUARIO")))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/encuestas/" + ACTOR + "/reservas/" + ACTOR + "/respuestas")
                .with(user(ACTOR).roles("USUARIO")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/encuestas").with(csrf()).with(user(ACTOR).roles("USUARIO")))
        .andExpect(status().isForbidden());
  }
}
