package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import edu.unse.sera.disponibilidad.persistence.DisponibilidadRepository;
import java.time.LocalTime;
import java.util.UUID;

public class DisponibilidadService {

  private final DisponibilidadRepository disponibilidadRepositoryRepository;

  public DisponibilidadService(DisponibilidadRepository disponibilidadRepositoryRepository) {
    this.disponibilidadRepositoryRepository = disponibilidadRepositoryRepository;
  }

  public DisponibilidadDetalle registrarDisponibilidad(
    DiaSemana diaSemana,
    LocalTime horaDesde, LocalTime horaHasta, UUID espacioId) {
    if (disponibilidadRepositoryRepository.existsByEspacioIdAndDiaSemana(espacioId,diaSemana)) {
      throw new DisponibilidadDuplicadaException(diaSemana,espacioId);
    }
    Disponibilidad disponibilidad = new Disponibilidad(diaSemana, horaDesde,horaHasta);
    return toResponse(disponibilidadRepositoryRepository.save(disponibilidad));
  }

  public Disponibilidad buscar(UUID id) {
    return disponibilidadRepositoryRepository.findById(id).orElseThrow(() -> new DisponibilidadNoEncontradaException(id));
  }

  public DisponibilidadDetalle actualizar(UUID id, LocalTime horaDesde, LocalTime horaHasta) {
    Disponibilidad disponibilidad = buscar(id);
    return toResponse(disponibilidad);
  }

  public DisponibilidadDetalle toResponse(Disponibilidad disponibilidad) {
    return new DisponibilidadDetalle(disponibilidad.getDiaSemana(),disponibilidad.getHoraDesde(),disponibilidad.getHoraHasta(),disponibilidad.getEspacio().getId());
  }

}
