package edu.unse.sera.membresia.persistence;

import edu.unse.sera.membresia.entity.Membresia;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a persistencia de membresías contratadas. */
public interface MembresiaRepository extends JpaRepository<Membresia, UUID> {
  // TODO(TRA-26): Extender JpaRepository<Membresia, UUID> después de agregar entidad y migración.
  // La primera consulta necesaria será obtener la membresía vigente de un socio.
}
