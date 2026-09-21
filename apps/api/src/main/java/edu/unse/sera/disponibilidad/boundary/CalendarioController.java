package edu.unse.sera.disponibilidad.boundary;

import edu.unse.sera.disponibilidad.control.CalendarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/espacios/{id}")
public class CalendarioController {
  private final CalendarioService service;

  public CalendarioController(CalendarioService service) {
    this.service = service;
  }

  public record BloqueoRequest(
      @NotNull LocalDate fecha,
      @NotNull LocalTime desde,
      @NotNull LocalTime hasta,
      @NotBlank @Size(max = 500) String motivo) {}

  @GetMapping("/calendario")
  public CalendarioService.Calendario consultar(
      @PathVariable UUID id, @RequestParam LocalDate fecha, Principal actor) {
    return service.consultar(id, fecha, UUID.fromString(actor.getName()));
  }

  @GetMapping("/bloqueos")
  public List<CalendarioService.BloqueoDetalle> listar(
      @PathVariable UUID id, @RequestParam LocalDate fecha) {
    return service.listar(id, fecha);
  }

  @PostMapping("/bloqueos")
  public ResponseEntity<CalendarioService.BloqueoDetalle> crear(
      @PathVariable UUID id, @Valid @RequestBody BloqueoRequest r) {
    return ResponseEntity.status(201)
        .body(service.bloquear(id, r.fecha(), r.desde(), r.hasta(), r.motivo()));
  }

  @DeleteMapping("/bloqueos/{bloqueoId}")
  public ResponseEntity<Void> eliminar(@PathVariable UUID id, @PathVariable UUID bloqueoId) {
    service.eliminar(id, bloqueoId);
    return ResponseEntity.noContent().build();
  }
}
