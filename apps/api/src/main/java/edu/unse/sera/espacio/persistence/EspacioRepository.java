package edu.unse.sera.espacio.persistence;

import edu.unse.sera.espacio.entity.Espacio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacioRepository extends JpaRepository<Espacio, UUID> {

  List<Espacio> findAllByOrderByNombreAsc();

  List<Espacio> findAllByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

  boolean existsByNombreIgnoreCase(String nombre);

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
