package edu.unse.sera.espacio.persistence;

import edu.unse.sera.espacio.entity.Espacio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacioRepository extends JpaRepository<Espacio, UUID> {

  //@Override
  //Optional<Espacio> findById(UUID id);

  List<Espacio> findAllByTipoContainingIgnoreCaseOrderByNombreAsc(String tipo);

  List<Espacio> findAllByOrderByNombreAsc();

  List<Espacio> findAllByNombreContainingIgnoreCase(String nombre);

  Espacio findByTipoContainingIgnoreCase(String tipo);

  boolean existsByNombreIgnoreCase(String nombre);


}
