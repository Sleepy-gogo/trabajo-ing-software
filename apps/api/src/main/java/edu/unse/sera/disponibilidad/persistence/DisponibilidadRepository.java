package edu.unse.sera.disponibilidad.persistence;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, UUID> {

  // @Override
  // Optional<Disponibilidad> findById(UUID id);

  List<Disponibilidad> findAllByEspacioId(UUID espacioId);

  Disponibilidad findByEspacioIdAndDiaSemana(UUID espacioId, DiaSemana diaSemana);

  boolean existsByEspacioIdAndDiaSemanaAndHoraDesdeLessThanAndHoraHastaGreaterThan(
      UUID espacioId, DiaSemana diaSemana, LocalTime horaHasta, LocalTime horaDesde);

  boolean existsByEspacioIdAndDiaSemana(UUID espacioId, DiaSemana diaSemana);
}
