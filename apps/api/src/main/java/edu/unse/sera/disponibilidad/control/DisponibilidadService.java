package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import edu.unse.sera.disponibilidad.persistence.DisponibilidadRepository;
import edu.unse.sera.espacio.control.EspacioNoEncontradoException;
import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisponibilidadService {

  private final DisponibilidadRepository disponibilidadRepository;
  private final EspacioRepository espacioRepository;

  public DisponibilidadService(
      DisponibilidadRepository disponibilidadRepository, EspacioRepository espacioRepository) {
    this.disponibilidadRepository = disponibilidadRepository;
    this.espacioRepository = espacioRepository;
  }

  public DisponibilidadDetalle registrarDisponibilidad(
      UUID espacioId, DiaSemana diaSemana, LocalTime horaDesde, LocalTime horaHasta) {
    Espacio espacio =
        espacioRepository
            .bloquearPorId(espacioId)
            .orElseThrow(() -> new EspacioNoEncontradoException(espacioId));
    if (disponibilidadRepository
        .existsByEspacioIdAndDiaSemanaAndHoraDesdeLessThanAndHoraHastaGreaterThan(
            espacioId, diaSemana, horaHasta, horaDesde)) {
      throw new DisponibilidadSuperpuestaException(diaSemana, espacioId);
    }
    Disponibilidad disponibilidad = new Disponibilidad(espacio, diaSemana, horaDesde, horaHasta);
    espacio.agregarDisponibilidad(disponibilidad);
    return toDetalle(disponibilidadRepository.save(disponibilidad));
  }

  @Transactional(readOnly = true)
  public List<DisponibilidadDetalle> listarPorEspacio(UUID espacioId) {
    buscarEspacio(espacioId);
    return disponibilidadRepository.findAllByEspacioId(espacioId).stream()
        .sorted(
            java.util.Comparator.comparingInt(
                    (Disponibilidad disponibilidad) -> disponibilidad.getDiaSemana().ordinal())
                .thenComparing(Disponibilidad::getHoraDesde))
        .map(this::toDetalle)
        .toList();
  }

  public DisponibilidadDetalle actualizar(
      UUID espacioId, UUID disponibilidadId, LocalTime horaDesde, LocalTime horaHasta) {
    espacioRepository
        .bloquearPorId(espacioId)
        .orElseThrow(() -> new EspacioNoEncontradoException(espacioId));
    Disponibilidad disponibilidad = buscar(espacioId, disponibilidadId);
    if (disponibilidadRepository
        .existsByEspacioIdAndDiaSemanaAndHoraDesdeLessThanAndHoraHastaGreaterThanAndIdNot(
            espacioId, disponibilidad.getDiaSemana(), horaHasta, horaDesde, disponibilidadId)) {
      throw new DisponibilidadSuperpuestaException(disponibilidad.getDiaSemana(), espacioId);
    }
    disponibilidad.actualizarDatos(horaDesde, horaHasta);
    return toDetalle(disponibilidad);
  }

  public void eliminar(UUID espacioId, UUID disponibilidadId) {
    disponibilidadRepository.delete(buscar(espacioId, disponibilidadId));
  }

  private Disponibilidad buscar(UUID espacioId, UUID disponibilidadId) {
    return disponibilidadRepository
        .findByIdAndEspacioId(disponibilidadId, espacioId)
        .orElseThrow(() -> new DisponibilidadNoEncontradaException(disponibilidadId));
  }

  private Espacio buscarEspacio(UUID espacioId) {
    return espacioRepository
        .findById(espacioId)
        .orElseThrow(() -> new EspacioNoEncontradoException(espacioId));
  }

  private DisponibilidadDetalle toDetalle(Disponibilidad disponibilidad) {
    return new DisponibilidadDetalle(
        disponibilidad.getId(),
        disponibilidad.getEspacio().getId(),
        disponibilidad.getDiaSemana(),
        disponibilidad.getHoraDesde(),
        disponibilidad.getHoraHasta());
  }
}
