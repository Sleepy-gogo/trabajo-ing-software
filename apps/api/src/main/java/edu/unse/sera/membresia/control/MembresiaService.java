package edu.unse.sera.membresia.control;

import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.membresia.persistence.NivelMembresiaRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembresiaService {
  private final NivelMembresiaRepository niveles;

  public MembresiaService(NivelMembresiaRepository niveles) {
    this.niveles = niveles;
  }

  @Transactional(readOnly = true)
  public List<NivelMembresiaDetalle> listarNiveles(boolean soloDisponibles) {
    return (soloDisponibles ? niveles.findAllByDisponibleParaContratar(true) : niveles.findAll())
        .stream()
            .sorted(java.util.Comparator.comparing(NivelMembresia::getNombre))
            .map(this::detalle)
            .toList();
  }

  @Transactional(readOnly = true)
  public NivelMembresiaDetalle obtenerNivel(UUID id) {
    return detalle(buscar(id));
  }

  public NivelMembresiaDetalle crearNivel(NivelMembresiaDatos datos) {
    if (niveles.existsByNombreIgnoreCase(datos.nombre().trim())) {
      throw new IllegalStateException("Ya existe un nivel con ese nombre.");
    }
    NivelMembresia nivel = new NivelMembresia(datos.nombre(), datos.descripcion());
    aplicar(nivel, datos);
    return detalle(niveles.save(nivel));
  }

  public NivelMembresiaDetalle actualizarNivel(UUID id, NivelMembresiaDatos datos) {
    if (niveles.existsByNombreIgnoreCaseAndIdNot(datos.nombre().trim(), id)) {
      throw new IllegalStateException("Ya existe un nivel con ese nombre.");
    }
    NivelMembresia nivel = buscar(id);
    aplicar(nivel, datos);
    return detalle(nivel);
  }

  public void deshabilitarNivel(UUID id) {
    buscar(id).deshabilitar();
  }

  private NivelMembresia buscar(UUID id) {
    return niveles.findById(id).orElseThrow(MembresiaNoEncontradaException::new);
  }

  private void aplicar(NivelMembresia n, NivelMembresiaDatos d) {
    n.actualizar(
        d.nombre(),
        d.descripcion(),
        d.preciosPorRelacion(),
        d.beneficios(),
        d.disponibleParaContratar());
  }

  private NivelMembresiaDetalle detalle(NivelMembresia n) {
    return new NivelMembresiaDetalle(
        n.getId(),
        n.getNombre(),
        n.getDescripcion(),
        Map.copyOf(n.getPreciosPorRelacion()),
        "ARS",
        List.copyOf(n.getBeneficios()),
        n.isDisponibleParaContratar());
  }
}
