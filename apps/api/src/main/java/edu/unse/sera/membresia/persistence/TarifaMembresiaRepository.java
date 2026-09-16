package edu.unse.sera.membresia.persistence;

/** Acceso a las tarifas de cada nivel según relación UNSE y fecha de vigencia. */
public interface TarifaMembresiaRepository {
  // TODO(TRA-25): Extender JpaRepository<TarifaMembresia, UUID> después de agregar entidad y
  // migración. Incluir una consulta para resolver la tarifa vigente por nivel, relación y fecha.
}
