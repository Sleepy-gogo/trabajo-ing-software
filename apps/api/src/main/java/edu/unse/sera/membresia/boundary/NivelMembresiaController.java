package edu.unse.sera.membresia.boundary;

import edu.unse.sera.membresia.boundary.dto.GuardarNivelMembresiaRequest;
import edu.unse.sera.membresia.boundary.dto.NivelMembresiaResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas de consulta y administración de niveles. Responden 501 hasta su implementación. */
@RestController
@RequestMapping("/api/niveles-membresia")
public class NivelMembresiaController {

  @GetMapping
  public ResponseEntity<List<NivelMembresiaResponse>> listar(
      @RequestParam(defaultValue = "true") boolean soloDisponibles) {
    // TODO(TRA-25): Delegar en MembresiaService y aplicar la tarifa por relación UNSE cuando se
    // conozca el usuario autenticado.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @GetMapping("/{nivelMembresiaId}")
  public ResponseEntity<NivelMembresiaResponse> obtener(@PathVariable UUID nivelMembresiaId) {
    // TODO(TRA-25): Delegar en MembresiaService y mapear nivel inexistente a 404.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @PostMapping
  public ResponseEntity<NivelMembresiaResponse> crear(
      @Valid @RequestBody GuardarNivelMembresiaRequest request) {
    // TODO(TRA-28): Mapear request a Control, crear nivel y tarifas, y responder 201 con Location.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @PutMapping("/{nivelMembresiaId}")
  public ResponseEntity<NivelMembresiaResponse> actualizar(
      @PathVariable UUID nivelMembresiaId,
      @Valid @RequestBody GuardarNivelMembresiaRequest request) {
    // TODO(TRA-28): Mapear request a Control y conservar tarifas históricas.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @DeleteMapping("/{nivelMembresiaId}")
  public ResponseEntity<Void> deshabilitar(@PathVariable UUID nivelMembresiaId) {
    // TODO(TRA-28): Dar de baja lógica al nivel y responder 204. No borrar niveles con historial.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
