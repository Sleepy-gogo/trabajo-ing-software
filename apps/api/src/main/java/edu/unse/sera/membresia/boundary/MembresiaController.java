package edu.unse.sera.membresia.boundary;

import edu.unse.sera.membresia.boundary.dto.CancelarMembresiaRequest;
import edu.unse.sera.membresia.boundary.dto.ContratarMembresiaRequest;
import edu.unse.sera.membresia.boundary.dto.MembresiaResponse;
import edu.unse.sera.membresia.control.MembresiaDetalle;
import edu.unse.sera.socio.control.SocioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membresias")
public class MembresiaController {
  private final SocioService service;

  public MembresiaController(SocioService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<MembresiaResponse> contratar(
      @Valid @RequestBody ContratarMembresiaRequest r, Principal actor) {
    var m =
        response(
            service.contratar(r.socioId(), r.nivelMembresiaId(), UUID.fromString(actor.getName())));
    return ResponseEntity.created(URI.create("/api/membresias/" + m.id())).body(m);
  }

  @GetMapping("/{id}")
  public MembresiaResponse obtener(@PathVariable UUID id, Principal actor) {
    return response(service.obtenerMembresia(id, UUID.fromString(actor.getName())));
  }

  @PostMapping("/{id}/cancelacion")
  public MembresiaResponse cancelar(
      @PathVariable UUID id, @Valid @RequestBody CancelarMembresiaRequest r, Principal actor) {
    return response(service.cancelar(id, r.motivo(), UUID.fromString(actor.getName())));
  }

  private MembresiaResponse response(MembresiaDetalle m) {
    return new MembresiaResponse(
        m.id(),
        m.socioId(),
        m.nivelMembresiaId(),
        m.nivelMembresiaNombre(),
        m.estado(),
        m.fechaAlta(),
        m.fechaBaja(),
        m.proximoVencimiento());
  }
}
