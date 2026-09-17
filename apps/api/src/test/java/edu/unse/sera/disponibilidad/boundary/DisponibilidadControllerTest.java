package edu.unse.sera.disponibilidad.boundary;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.disponibilidad.control.DisponibilidadDetalle;
import edu.unse.sera.disponibilidad.control.DisponibilidadService;
import edu.unse.sera.disponibilidad.control.DisponibilidadSuperpuestaException;
import edu.unse.sera.disponibilidad.entity.DiaSemana;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DisponibilidadController.class)
class DisponibilidadControllerTest {

  private final MockMvc mockMvc;

  @MockitoBean private DisponibilidadService disponibilidadService;

  @Autowired
  DisponibilidadControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void creaUnaDisponibilidadAnidadaEnElEspacio() throws Exception {
    UUID espacioId = UUID.randomUUID();
    UUID disponibilidadId = UUID.randomUUID();
    when(disponibilidadService.registrarDisponibilidad(
            espacioId, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0)))
        .thenReturn(detalle(disponibilidadId, espacioId));

    mockMvc
        .perform(
            post("/api/espacios/{espacioId}/disponibilidades", espacioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"diaSemana":"LUNES","horaDesde":"08:00","horaHasta":"12:00"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    "/api/espacios/" + espacioId + "/disponibilidades/" + disponibilidadId))
        .andExpect(jsonPath("$.id").value(disponibilidadId.toString()))
        .andExpect(jsonPath("$.diaSemana").value("LUNES"));
  }

  @Test
  void listaLaDisponibilidadDeUnEspacio() throws Exception {
    UUID espacioId = UUID.randomUUID();
    UUID disponibilidadId = UUID.randomUUID();
    when(disponibilidadService.listarPorEspacio(espacioId))
        .thenReturn(List.of(detalle(disponibilidadId, espacioId)));

    mockMvc
        .perform(get("/api/espacios/{espacioId}/disponibilidades", espacioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].espacioId").value(espacioId.toString()))
        .andExpect(jsonPath("$[0].horaDesde").value("08:00:00"));
  }

  @Test
  void actualizaLasHorasSinCambiarElDia() throws Exception {
    UUID espacioId = UUID.randomUUID();
    UUID disponibilidadId = UUID.randomUUID();
    when(disponibilidadService.actualizar(
            espacioId, disponibilidadId, LocalTime.of(9, 0), LocalTime.of(13, 0)))
        .thenReturn(
            new DisponibilidadDetalle(
                disponibilidadId,
                espacioId,
                DiaSemana.LUNES,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)));

    mockMvc
        .perform(
            put(
                    "/api/espacios/{espacioId}/disponibilidades/{disponibilidadId}",
                    espacioId,
                    disponibilidadId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaDesde\":\"09:00\",\"horaHasta\":\"13:00\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.horaDesde").value("09:00:00"))
        .andExpect(jsonPath("$.diaSemana").value("LUNES"));
  }

  @Test
  void eliminaUnaDisponibilidad() throws Exception {
    UUID espacioId = UUID.randomUUID();
    UUID disponibilidadId = UUID.randomUUID();

    mockMvc
        .perform(
            delete(
                "/api/espacios/{espacioId}/disponibilidades/{disponibilidadId}",
                espacioId,
                disponibilidadId))
        .andExpect(status().isNoContent());

    verify(disponibilidadService).eliminar(espacioId, disponibilidadId);
  }

  @Test
  void informaComoConflictoUnRangoSuperpuesto() throws Exception {
    UUID espacioId = UUID.randomUUID();
    when(disponibilidadService.registrarDisponibilidad(
            espacioId, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0)))
        .thenThrow(new DisponibilidadSuperpuestaException(DiaSemana.LUNES, espacioId));

    mockMvc
        .perform(
            post("/api/espacios/{espacioId}/disponibilidades", espacioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"diaSemana":"LUNES","horaDesde":"08:00","horaHasta":"12:00"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("disponibilidad_superpuesta"));
  }

  @Test
  void rechazaUnRangoInvertidoAntesDeLlamarAlService() throws Exception {
    UUID espacioId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/espacios/{espacioId}/disponibilidades", espacioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"diaSemana":"LUNES","horaDesde":"12:00","horaHasta":"08:00"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("datos_invalidos"));

    verify(disponibilidadService, org.mockito.Mockito.never())
        .registrarDisponibilidad(
            espacioId, DiaSemana.LUNES, LocalTime.of(12, 0), LocalTime.of(8, 0));
  }

  private DisponibilidadDetalle detalle(UUID disponibilidadId, UUID espacioId) {
    return new DisponibilidadDetalle(
        disponibilidadId, espacioId, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0));
  }
}
