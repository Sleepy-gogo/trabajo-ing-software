package edu.unse.sera.encuesta.persistence;

import edu.unse.sera.encuesta.entity.Encuesta;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EncuestaRepository extends JpaRepository<Encuesta, UUID> {
  List<Encuesta> findAllByOrderByCreadaEnDesc();

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Encuesta e where e.id = :id")
  java.util.Optional<Encuesta> bloquear(UUID id);
}
