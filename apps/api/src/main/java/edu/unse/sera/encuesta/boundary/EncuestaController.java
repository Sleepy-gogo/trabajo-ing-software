package edu.unse.sera.encuesta.boundary;

import edu.unse.sera.encuesta.control.EncuestaDatos;
import edu.unse.sera.encuesta.control.EncuestaDetalle;
import edu.unse.sera.encuesta.control.EncuestaService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/encuestas")
public class EncuestaController {
  private final EncuestaService encuestas;

  public EncuestaController(EncuestaService encuestas) {
    this.encuestas = encuestas;
  }

  @GetMapping
  public List<EncuestaDetalle> listar() {
    return encuestas.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EncuestaDetalle crear(@RequestBody EncuestaDatos datos) {
    return encuestas.crear(datos);
  }

  public record EstadoRequest(boolean activa) {}

  @PutMapping("/{id}/estado")
  public EncuestaDetalle estado(@PathVariable UUID id, @RequestBody EstadoRequest datos) {
    return encuestas.cambiarActiva(id, datos.activa());
  }

  @GetMapping("/{id}/resultados")
  public EncuestaDetalle.Resultados resultados(@PathVariable UUID id) {
    return encuestas.resultados(id);
  }

  @GetMapping("/me")
  public List<EncuestaDetalle.Asignacion> disponibles(Principal actor) {
    return encuestas.disponibles(UUID.fromString(actor.getName()));
  }

  @GetMapping("/{id}/reservas/{reservaId}")
  public EncuestaDetalle.Asignacion consultar(
      @PathVariable UUID id, @PathVariable UUID reservaId, Principal actor) {
    return encuestas.consultar(id, reservaId, UUID.fromString(actor.getName()));
  }

  public record RespuestasRequest(Map<UUID, String> respuestas) {}

  @PostMapping("/{id}/reservas/{reservaId}/respuestas")
  @ResponseStatus(HttpStatus.CREATED)
  public EncuestaDetalle.Asignacion responder(
      @PathVariable UUID id,
      @PathVariable UUID reservaId,
      @RequestBody RespuestasRequest datos,
      Principal actor) {
    return encuestas.responder(id, reservaId, UUID.fromString(actor.getName()), datos.respuestas());
  }
}
