package edu.unse.sera.disponibilidad.persistence;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, UUID> {

  List<Disponibilidad> findAllByEspacioId(UUID espacioId);

  Optional<Disponibilidad> findByIdAndEspacioId(UUID id, UUID espacioId);

  boolean existsByEspacioIdAndDiaSemanaAndHoraDesdeLessThanAndHoraHastaGreaterThan(
      UUID espacioId, DiaSemana diaSemana, LocalTime horaHasta, LocalTime horaDesde);

  boolean existsByEspacioIdAndDiaSemanaAndHoraDesdeLessThanAndHoraHastaGreaterThanAndIdNot(
      UUID espacioId, DiaSemana diaSemana, LocalTime horaHasta, LocalTime horaDesde, UUID id);
}
