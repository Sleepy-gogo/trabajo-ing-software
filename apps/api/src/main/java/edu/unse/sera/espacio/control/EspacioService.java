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

  public EspacioDetalle crear(String nombre, String descripcion) {
    Espacio espacio = new Espacio(nombre, descripcion);
    return toResponse(espacioRepository.save(espacio));
  }

  @Transactional(readOnly = true)
  public List<EspacioDetalle> listar() {
    return espacioRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public EspacioDetalle obtener(UUID id) {
    return toResponse(buscar(id));
  }

  public EspacioDetalle actualizar(UUID id, String nombre, String descripcion) {
    Espacio espacio = buscar(id);
    espacio.actualizar(nombre, descripcion);
    return toResponse(espacio);
  }

  public void eliminar(UUID id) {
    espacioRepository.delete(buscar(id));
  }

  private Espacio buscar(UUID id) {
    return espacioRepository.findById(id).orElseThrow(() -> new EspacioNoEncontradoException(id));
  }

  private EspacioDetalle toResponse(Espacio espacio) {
    return new EspacioDetalle(espacio.getId(), espacio.getNombre(), espacio.getDescripcion());
  }
}
