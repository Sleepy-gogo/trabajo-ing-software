package edu.unse.sera.membresia.boundary;

import edu.unse.sera.membresia.boundary.dto.GuardarNivelMembresiaRequest;
import edu.unse.sera.membresia.boundary.dto.NivelMembresiaResponse;
import edu.unse.sera.membresia.control.MembresiaService;
import edu.unse.sera.membresia.control.NivelMembresiaDatos;
import edu.unse.sera.membresia.control.NivelMembresiaDetalle;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/niveles-membresia")
public class NivelMembresiaController {
  private final MembresiaService service;

  public NivelMembresiaController(MembresiaService service) {
    this.service = service;
  }

  @GetMapping
  public List<NivelMembresiaResponse> listar(
      @RequestParam(defaultValue = "true") boolean soloDisponibles) {
    return service.listarNiveles(soloDisponibles).stream().map(this::response).toList();
  }

  @GetMapping("/{id}")
  public NivelMembresiaResponse obtener(@PathVariable UUID id) {
    return response(service.obtenerNivel(id));
  }

  @PostMapping
  public ResponseEntity<NivelMembresiaResponse> crear(
      @Valid @RequestBody GuardarNivelMembresiaRequest r) {
    var n = response(service.crearNivel(datos(r)));
    return ResponseEntity.created(URI.create("/api/niveles-membresia/" + n.id())).body(n);
  }

  @PutMapping("/{id}")
  public NivelMembresiaResponse actualizar(
      @PathVariable UUID id, @Valid @RequestBody GuardarNivelMembresiaRequest r) {
    return response(service.actualizarNivel(id, datos(r)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
    service.deshabilitarNivel(id);
    return ResponseEntity.noContent().build();
  }

  private NivelMembresiaDatos datos(GuardarNivelMembresiaRequest r) {
    return new NivelMembresiaDatos(
        r.nombre(),
        r.descripcion(),
        r.preciosPorRelacion(),
        r.beneficios(),
        r.disponibleParaContratar());
  }

  private NivelMembresiaResponse response(NivelMembresiaDetalle n) {
    return new NivelMembresiaResponse(
        n.id(),
        n.nombre(),
        n.descripcion(),
        n.preciosPorRelacion(),
        n.moneda(),
        n.beneficios(),
        n.disponibleParaContratar());
  }
}
