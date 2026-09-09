package edu.unse.sera.espacio.boundary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.espacio.control.EspacioDetalle;
import edu.unse.sera.espacio.control.EspacioService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EspacioController.class)
class EspacioControllerTest {

  private final MockMvc mockMvc;

  @MockitoBean private EspacioService espacioService;

  @Autowired
  EspacioControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void creaUnEspacio() throws Exception {
    UUID id = UUID.randomUUID();
    when(espacioService.crear(eq("Cancha cubierta"), any()))
        .thenReturn(new EspacioDetalle(id, "Cancha cubierta", null));

    mockMvc
        .perform(
            post("/api/espacios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nombre":"Cancha cubierta","descripcion":null}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/espacios/" + id))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Cancha cubierta"));
  }

  @Test
  void rechazaUnNombreVacio() throws Exception {
    mockMvc
        .perform(
            post("/api/espacios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nombre":" ","descripcion":null}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("datos_invalidos"))
        .andExpect(jsonPath("$.campos.nombre").value("El nombre es obligatorio."));
  }
}
