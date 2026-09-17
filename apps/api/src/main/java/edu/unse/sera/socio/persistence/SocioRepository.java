package edu.unse.sera.socio.persistence;

import edu.unse.sera.socio.entity.Socio;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a persistencia de socios. */
public interface SocioRepository extends JpaRepository<Socio, UUID> {
  // TODO(TRA-26): Extender JpaRepository<Socio, UUID> cuando Socio tenga mapeo JPA y migración.
  // Agregar únicamente las consultas que necesiten SocioService; evitar una capa DAO adicional.
}
