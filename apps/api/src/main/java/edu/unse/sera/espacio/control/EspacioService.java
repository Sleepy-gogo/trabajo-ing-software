package edu.unse.sera.espacio.control;

import edu.unse.sera.disponibilidad.control.DisponibilidadDetalle;
import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import java.math.BigDecimal;
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

  public EspacioDetalle registrarEspacio(
      String nombre,
      String descripcion,
      int capacidad,
      BigDecimal tarifaHora,
      String tipo,
      String rutaImagen) {
    Espacio espacio = new Espacio(nombre, descripcion, capacidad, tarifaHora, tipo, rutaImagen);
    if (espacioRepository.existsByNombreIgnoreCase(espacio.getNombre())) {
      throw new EspacioDuplicadoException(espacio.getNombre());
    }
    return toResponse(espacioRepository.save(espacio));
  }

  @Transactional(readOnly = true)
  public EspacioDetalle consultarDetalle(UUID id) {
    return toResponse(buscar(id));
  }

  public EspacioDetalle actualizar(
      UUID id,
      String nombre,
      String descripcion,
      int capacidad,
      BigDecimal tarifaHora,
      String tipo,
      String rutaImagen) {
    Espacio espacio = buscar(id);
    String nombreNormalizado = nombre == null ? null : nombre.trim();
    if (espacioRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, id)) {
      throw new EspacioDuplicadoException(nombreNormalizado);
    }
    espacio.actualizar(nombre, descripcion, capacidad, tarifaHora, tipo, rutaImagen);
    return toResponse(espacio);
  }

  public void eliminar(UUID id) {
    espacioRepository.delete(buscar(id));
  }

  private Espacio buscar(UUID id) {
    return espacioRepository.findById(id).orElseThrow(() -> new EspacioNoEncontradoException(id));
  }

  /** Reúne el listado administrativo y su búsqueda en un único caso de uso. */
  @Transactional(readOnly = true)
  public List<EspacioDetalle> listar(String criterio) {
    List<Espacio> espacios;
    if (criterio == null || criterio.isBlank()) {
      espacios = espacioRepository.findAllByOrderByNombreAsc();
    } else {
      String valor = criterio.trim();
      espacios = espacioRepository.findAllByNombreContainingIgnoreCaseOrderByNombreAsc(valor);
    }
    return espacios.stream().map(this::toResponse).toList();
  }

  private EspacioDetalle toResponse(Espacio espacio) {
    return new EspacioDetalle(
        espacio.getId(),
        espacio.getNombre(),
        espacio.getDescripcion(),
        espacio.getCapacidad(),
        espacio.getTarifaHora(),
        espacio.getTipo(),
        espacio.getRutaImagen(),
        espacio.getDisponibilidad().stream()
            .map(
                disponibilidad ->
                    new DisponibilidadDetalle(
                        disponibilidad.getId(),
                        espacio.getId(),
                        disponibilidad.getDiaSemana(),
                        disponibilidad.getHoraDesde(),
                        disponibilidad.getHoraHasta()))
            .toList(),
        espacio.getCreatedAt(),
        espacio.getUpdatedAt());
  }
}
