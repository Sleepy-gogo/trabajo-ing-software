package edu.unse.sera.espacio.control;

import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EspacioService {

  private final EspacioRepository espacioRepository;

  public EspacioService(EspacioRepository espacioRepository) {
    this.espacioRepository = espacioRepository;
  }

  public EspacioDetalle registrarEspacio(String nombre, String descripcion, int capacidad,
    String tipo, String rutaImagen) {
    if (espacioRepository.existsByNombreIgnoreCase(nombre)) {
      throw new EspacioDuplicadoException(nombre);
    }
    Espacio espacio = new Espacio(nombre, descripcion, capacidad, tipo, rutaImagen);
    return toResponse(espacioRepository.save(espacio));
  }

  @Transactional(readOnly = true)
  public List<EspacioDetalle> listar() {
    return espacioRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public EspacioDetalle consultarDetalle(UUID id) {
    return toResponse(buscar(id));
  }

  public EspacioDetalle actualizar(UUID id, String nombre, String descripcion, int capacidad,
    String tipo, String rutaImagen) {
    Espacio espacio = buscar(id);
    espacio.actualizar(nombre, descripcion, capacidad, tipo, rutaImagen);
    return toResponse(espacio);
  }

  public void eliminar(UUID id) {
    espacioRepository.delete(buscar(id));
  }


  private Espacio buscar(UUID id) {
    return espacioRepository.findById(id).orElseThrow(() -> new EspacioNoEncontradoException(id));
  }


  /**
   * Reúne el listado administrativo y su búsqueda en un único caso de uso.
   */
  @Transactional(readOnly = true)
  public List<EspacioDetalle> listar(String criterio) {
    List<Espacio> espacios;
    if (criterio == null || criterio.isBlank()) {
      espacios = espacioRepository.findAllByOrderByNombreAsc();
    } else {
      String valor = criterio.trim();
      espacios = espacioRepository.findAllByNombreContainingIgnoreCase(valor);
      //espacios = espacioRepository.findAllByTipoContainingIgnoreCaseOrderByNombreAsc(valor);
    }
    return espacios.stream().map(this::toResponse).toList();
  }

  private EspacioDetalle toResponse(Espacio espacio) {
    return new EspacioDetalle(espacio.getId(), espacio.getNombre(), espacio.getDescripcion(),
      espacio.getCapacidad(), espacio.getTipo(), espacio.getRutaImagen());
  }
}
