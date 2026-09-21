package edu.unse.sera.disponibilidad.persistence;

import edu.unse.sera.disponibilidad.entity.Bloqueo;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoRepository extends JpaRepository<Bloqueo, UUID> {
  List<Bloqueo> findAllByEspacioIdAndFechaOrderByDesde(UUID espacioId, LocalDate fecha);
}
