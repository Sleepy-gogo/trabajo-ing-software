package edu.unse.sera.espacio.persistence;

import edu.unse.sera.espacio.entity.Espacio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacioRepository extends JpaRepository<Espacio, UUID> {

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select e from Espacio e where e.id = :id")
  java.util.Optional<Espacio> bloquearPorId(
      @org.springframework.data.repository.query.Param("id") UUID id);

  List<Espacio> findAllByOrderByNombreAsc();

  List<Espacio> findAllByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

  boolean existsByNombreIgnoreCase(String nombre);

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
