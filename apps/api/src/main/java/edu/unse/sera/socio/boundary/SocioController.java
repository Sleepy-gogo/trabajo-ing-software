package edu.unse.sera.socio.boundary;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.socio.boundary.dto.ActualizarSocioRequest;
import edu.unse.sera.socio.boundary.dto.RegistrarSocioRequest;
import edu.unse.sera.socio.boundary.dto.SocioResponse;
import edu.unse.sera.socio.control.CambioSocioDetalle;
import edu.unse.sera.socio.control.SocioDetalle;
import edu.unse.sera.socio.control.SocioService;
import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/socios")
public class SocioController {
  private final SocioService service;

  public SocioController(SocioService service) {
    this.service = service;
  }

  @GetMapping("/me")
  public ResponseEntity<SocioResponse> actual(Principal actor) {
    var s = service.actual(UUID.fromString(actor.getName()));
    return s == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response(s));
  }

  @PostMapping
  public ResponseEntity<SocioResponse> crear(
      @Valid @RequestBody RegistrarSocioRequest r, Principal actor) {
    var s =
        response(
            service.registrar(
                r.usuarioId(),
                r.relacionUnse(),
                r.identificadorUnse(),
                r.nivelMembresiaId(),
                UUID.fromString(actor.getName())));
    return ResponseEntity.created(URI.create("/api/socios/" + s.id())).body(s);
  }

  @GetMapping
  public List<SocioResponse> listar(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) EstadoMembresia estadoMembresia,
      @RequestParam(required = false) RelacionUnse relacionUnse) {
    return service.listar(buscar, estadoMembresia, relacionUnse).stream()
        .map(this::response)
        .toList();
  }

  @GetMapping("/{id}")
  public SocioResponse obtener(@PathVariable UUID id, Principal actor) {
    return response(service.consultar(id, UUID.fromString(actor.getName())));
  }

  @PutMapping("/{id}")
  public SocioResponse actualizar(
      @PathVariable UUID id, @Valid @RequestBody ActualizarSocioRequest r, Principal actor) {
    return response(
        service.actualizar(
            id,
            r.relacionUnse(),
            r.estadoVerificacionUnse(),
            r.identificadorUnse(),
            r.nivelMembresiaId(),
            r.estadoMembresia(),
            r.motivo(),
            UUID.fromString(actor.getName())));
  }

  @GetMapping("/{id}/historial")
  public List<CambioSocioDetalle> historial(@PathVariable UUID id) {
    return service.historial(id);
  }

  private SocioResponse response(SocioDetalle s) {
    return new SocioResponse(
        s.id(),
        s.usuarioId(),
        s.nombreCompleto(),
        s.email(),
        s.dni(),
        s.relacionUnse(),
        s.estadoVerificacionUnse(),
        s.identificadorUnse(),
        s.membresiaId(),
        s.nivelMembresiaId(),
        s.nivelMembresiaNombre(),
        s.estadoMembresia(),
        s.proximoVencimiento());
  }
}
