package edu.unse.sera.membresia.persistence;

/** Acceso a persistencia de membresías contratadas. */
public interface MembresiaRepository {
  // TODO(TRA-26): Extender JpaRepository<Membresia, UUID> después de agregar entidad y migración.
  // La primera consulta necesaria será obtener la membresía vigente de un socio.
}
