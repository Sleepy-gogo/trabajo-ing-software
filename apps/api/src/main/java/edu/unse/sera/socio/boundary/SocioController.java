package edu.unse.sera.socio.boundary;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.socio.boundary.dto.ActualizarSocioRequest;
import edu.unse.sera.socio.boundary.dto.RegistrarSocioRequest;
import edu.unse.sera.socio.boundary.dto.SocioResponse;
import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas administrativas de socios. Todas responden 501 hasta su implementación. */
@RestController
@RequestMapping("/api/socios")
public class SocioController {

  @PostMapping
  public ResponseEntity<SocioResponse> registrar(
      @Valid @RequestBody RegistrarSocioRequest request) {
    // TODO(TRA-26): Delegar en SocioService y responder 201 con Location.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @GetMapping
  public ResponseEntity<List<SocioResponse>> listar(
      @RequestParam(name = "buscar", required = false) String criterio,
      @RequestParam(required = false) EstadoMembresia estadoMembresia,
      @RequestParam(required = false) RelacionUnse relacionUnse) {
    // TODO(TRA-28): Delegar filtros en Control y convertir SocioDetalle a response DTO.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @GetMapping("/{socioId}")
  public ResponseEntity<SocioResponse> obtener(@PathVariable UUID socioId) {
    // TODO(TRA-26): Delegar en SocioService y mapear socio inexistente a 404.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @PutMapping("/{socioId}")
  public ResponseEntity<SocioResponse> actualizar(
      @PathVariable UUID socioId, @Valid @RequestBody ActualizarSocioRequest request) {
    // TODO(TRA-27): Delegar en SocioService. La autorización administrativa queda fuera de este
    // ticket hasta que exista autenticación.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
