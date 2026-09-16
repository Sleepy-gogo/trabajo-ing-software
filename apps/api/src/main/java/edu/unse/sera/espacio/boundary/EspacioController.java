package edu.unse.sera.espacio.boundary;

import edu.unse.sera.espacio.boundary.dto.EspacioResponse;
import edu.unse.sera.espacio.boundary.dto.GuardarEspacioRequest;
import edu.unse.sera.espacio.control.EspacioDetalle;
import edu.unse.sera.espacio.control.EspacioService;
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
@RequestMapping("/api/espacios")
public class EspacioController {

  private final EspacioService espacioService;

  public EspacioController(EspacioService espacioService) {
    this.espacioService = espacioService;
  }

  @PostMapping
  public ResponseEntity<EspacioResponse> crear(@Valid @RequestBody GuardarEspacioRequest request) {
    EspacioResponse response =
        toResponse(
            espacioService.registrarEspacio(
                request.nombre(),
                request.descripcion(),
                request.capacidad(),
                request.tarifaHora(),
                request.tipo(),
                request.rutaImagen()));
    return ResponseEntity.created(URI.create("/api/espacios/" + response.id())).body(response);
  }

  @GetMapping
  public List<EspacioResponse> listar() {
    return espacioService.listar().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  public EspacioResponse obtener(@PathVariable UUID id) {
    return toResponse(espacioService.consultarDetalle(id));
  }

  @PutMapping("/{id}")
  public EspacioResponse actualizar(
      @PathVariable UUID id, @Valid @RequestBody GuardarEspacioRequest request) {
    return toResponse(
        espacioService.actualizar(
            id,
            request.nombre(),
            request.descripcion(),
            request.capacidad(),
            request.tarifaHora(),
            request.tipo(),
            request.rutaImagen()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
    espacioService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  private EspacioResponse toResponse(EspacioDetalle espacio) {
    return new EspacioResponse(
        espacio.id(),
        espacio.nombre(),
        espacio.descripcion(),
        espacio.capacidad(),
        espacio.tarifaHora(),
        espacio.tipo(),
        espacio.rutaImagen(),
        espacio.disponibilidadList());
  }
}
