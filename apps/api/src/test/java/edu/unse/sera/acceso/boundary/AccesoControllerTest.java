package edu.unse.sera.acceso.boundary;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.acceso.control.AccesoResultado;
import edu.unse.sera.acceso.control.AccesoService;
import edu.unse.sera.shared.config.SecurityConfig;
import edu.unse.sera.usuario.control.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccesoController.class)
@Import(SecurityConfig.class)
class AccesoControllerTest {
  private final MockMvc mvc;
  @MockitoBean private AccesoService accesos;
  @MockitoBean private UsuarioService usuarios;

  @Autowired
  AccesoControllerTest(MockMvc mvc) {
    this.mvc = mvc;
  }

  @Test
  void requiereSesionYRolOperador() throws Exception {
    mvc.perform(
            post("/api/accesos/validacion")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"SERA-x\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/accesos/validacion")
                .with(csrf())
                .with(user("socio").roles("USUARIO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"SERA-x\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(accesos);
  }

  @Test
  void permiteStaffYAdminConCsrf() throws Exception {
    when(accesos.validar("SERA-x"))
        .thenReturn(
            new AccesoResultado(
                false, "Código no reconocido.", "DESCONOCIDO", null, null, null, null, null));
    for (String rol : new String[] {"STAFF", "ADMIN"}) {
      mvc.perform(
              post("/api/accesos/validacion")
                  .with(csrf())
                  .with(user("operador").roles(rol))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"codigo\":\"SERA-x\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.autorizado").value(false));
    }
  }

  @Test
  void rechazaSinCsrfYEntradaVacia() throws Exception {
    mvc.perform(
            post("/api/accesos/validacion")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"SERA-x\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/accesos/validacion")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\" \"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(accesos);
  }
}
