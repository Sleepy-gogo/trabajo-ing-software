package edu.unse.sera.disponibilidad.boundary;

import edu.unse.sera.disponibilidad.boundary.dto.ActualizarDisponibilidadRequest;
import edu.unse.sera.disponibilidad.boundary.dto.CrearDisponibilidadRequest;
import edu.unse.sera.disponibilidad.boundary.dto.DisponibilidadResponse;
import edu.unse.sera.disponibilidad.control.DisponibilidadDetalle;
import edu.unse.sera.disponibilidad.control.DisponibilidadService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/espacios/{espacioId}/disponibilidades")
public class DisponibilidadController {

  private final DisponibilidadService disponibilidadService;

  public DisponibilidadController(DisponibilidadService disponibilidadService) {
    this.disponibilidadService = disponibilidadService;
  }

  @PostMapping
  public ResponseEntity<DisponibilidadResponse> crear(
      @PathVariable UUID espacioId, @Valid @RequestBody CrearDisponibilidadRequest request) {
    DisponibilidadResponse response =
        toResponse(
            disponibilidadService.registrarDisponibilidad(
                espacioId, request.diaSemana(), request.horaDesde(), request.horaHasta()));
    return ResponseEntity.created(
            URI.create("/api/espacios/" + espacioId + "/disponibilidades/" + response.id()))
        .body(response);
  }

  @GetMapping
  public List<DisponibilidadResponse> listar(@PathVariable UUID espacioId) {
    return disponibilidadService.listarPorEspacio(espacioId).stream()
        .map(this::toResponse)
        .toList();
  }

  @PutMapping("/{disponibilidadId}")
  public DisponibilidadResponse actualizar(
      @PathVariable UUID espacioId,
      @PathVariable UUID disponibilidadId,
      @Valid @RequestBody ActualizarDisponibilidadRequest request) {
    return toResponse(
        disponibilidadService.actualizar(
            espacioId, disponibilidadId, request.horaDesde(), request.horaHasta()));
  }

  @DeleteMapping("/{disponibilidadId}")
  public ResponseEntity<Void> eliminar(
      @PathVariable UUID espacioId, @PathVariable UUID disponibilidadId) {
    disponibilidadService.eliminar(espacioId, disponibilidadId);
    return ResponseEntity.noContent().build();
  }

  private DisponibilidadResponse toResponse(DisponibilidadDetalle disponibilidad) {
    return new DisponibilidadResponse(
        disponibilidad.id(),
        disponibilidad.espacioId(),
        disponibilidad.diaSemana(),
        disponibilidad.horaDesde(),
        disponibilidad.horaHasta());
  }
}
