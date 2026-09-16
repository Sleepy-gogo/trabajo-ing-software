package edu.unse.sera.socio.persistence;

/** Acceso a persistencia de socios. */
public interface SocioRepository {
  // TODO(TRA-26): Extender JpaRepository<Socio, UUID> cuando Socio tenga mapeo JPA y migración.
  // Agregar únicamente las consultas que necesiten SocioService; evitar una capa DAO adicional.
}
