package edu.unse.sera.espacio.boundary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.espacio.control.EspacioDetalle;
import edu.unse.sera.espacio.control.EspacioNoEncontradoException;
import edu.unse.sera.espacio.control.EspacioService;
import java.util.List;
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
    when(espacioService.registrarEspacio(eq("Cancha cubierta"), any()))
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

  @Test
  void listaLosEspacios() throws Exception {
    UUID id = UUID.randomUUID();
    when(espacioService.listar())
        .thenReturn(List.of(new EspacioDetalle(id, "Cancha cubierta", null)));

    mockMvc
        .perform(get("/api/espacios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].nombre").value("Cancha cubierta"));
  }

  @Test
  void respondeNotFoundCuandoElEspacioNoExiste() throws Exception {
    UUID id = UUID.randomUUID();
    when(espacioService.consultarDetalle(id)).thenThrow(new EspacioNoEncontradoException(id));

    mockMvc
        .perform(get("/api/espacios/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("recurso_no_encontrado"))
        .andExpect(jsonPath("$.mensaje").value("No existe el espacio con id " + id + "."));
  }

  @Test
  void actualizaUnEspacio() throws Exception {
    UUID id = UUID.randomUUID();
    when(espacioService.actualizar(id, "Cancha norte", "Piso renovado"))
        .thenReturn(new EspacioDetalle(id, "Cancha norte", "Piso renovado"));

    mockMvc
        .perform(
            put("/api/espacios/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
              {"nombre":"Cancha norte","descripcion":"Piso renovado"}
              """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Cancha norte"))
        .andExpect(jsonPath("$.descripcion").value("Piso renovado"));
  }

  @Test
  void eliminaUnEspacio() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/espacios/{id}", id)).andExpect(status().isNoContent());

    verify(espacioService).eliminar(id);
  }
}
