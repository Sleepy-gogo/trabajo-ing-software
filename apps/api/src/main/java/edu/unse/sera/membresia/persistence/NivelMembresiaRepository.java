package edu.unse.sera.membresia.persistence;

import edu.unse.sera.membresia.entity.NivelMembresia;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a persistencia de niveles de membresía. */
public interface NivelMembresiaRepository extends JpaRepository<NivelMembresia, UUID> {
  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

  boolean existsByNombreIgnoreCase(String nombre);

  List<NivelMembresia> findAllByDisponibleParaContratar(boolean disponibleParaContratar);
}
