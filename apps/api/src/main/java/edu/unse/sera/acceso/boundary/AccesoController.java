package edu.unse.sera.acceso.boundary;

import edu.unse.sera.acceso.control.AccesoResultado;
import edu.unse.sera.acceso.control.AccesoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accesos")
public class AccesoController {
  private final AccesoService accesos;

  public AccesoController(AccesoService accesos) {
    this.accesos = accesos;
  }

  public record ValidarRequest(@NotBlank @Size(max = 128) String codigo) {}

  @PostMapping("/validacion")
  public AccesoResultado validar(@Valid @RequestBody ValidarRequest request) {
    return accesos.validar(request.codigo());
  }
}
