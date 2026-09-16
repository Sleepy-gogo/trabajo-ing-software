package edu.unse.sera.membresia.boundary;

import edu.unse.sera.membresia.boundary.dto.CancelarMembresiaRequest;
import edu.unse.sera.membresia.boundary.dto.ContratarMembresiaRequest;
import edu.unse.sera.membresia.boundary.dto.MembresiaResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas de contratación, consulta, modificación y cancelación de membresías. */
@RestController
@RequestMapping("/api/membresias")
public class MembresiaController {

  @PostMapping
  public ResponseEntity<MembresiaResponse> contratar(
      @Valid @RequestBody ContratarMembresiaRequest request) {
    // TODO(TRA-26): Delegar en MembresiaService y responder 201 con Location. La membresía nace en
    // PENDIENTE_PAGO; no asumir que la salida del proveedor equivale a un pago aprobado.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @GetMapping("/{membresiaId}")
  public ResponseEntity<MembresiaResponse> obtener(@PathVariable UUID membresiaId) {
    // TODO(TRA-26): Delegar en MembresiaService y mapear membresía inexistente a 404.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @PostMapping("/{membresiaId}/cancelacion")
  public ResponseEntity<MembresiaResponse> cancelar(
      @PathVariable UUID membresiaId, @Valid @RequestBody CancelarMembresiaRequest request) {
    // TODO(TRA-27): Delegar en MembresiaService. La UI debe pedir confirmación antes de llamar a
    // esta ruta, pero Control debe validar el estado aunque el cliente omita esa confirmación.
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
