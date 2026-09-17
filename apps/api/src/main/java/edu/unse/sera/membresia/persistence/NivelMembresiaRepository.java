package edu.unse.sera.membresia.persistence;

import edu.unse.sera.membresia.entity.NivelMembresia;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a persistencia de niveles de membresía. */
public interface NivelMembresiaRepository extends JpaRepository<NivelMembresia, UUID> {
  // TODO(TRA-25): Extender JpaRepository<NivelMembresia, UUID> después de agregar entidad y
  // migración. El listado público debe poder filtrar por disponibleParaContratar.
}
