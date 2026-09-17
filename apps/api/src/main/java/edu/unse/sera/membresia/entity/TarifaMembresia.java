package edu.unse.sera.membresia.entity;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Precio mensual de un nivel para una relación con la UNSE durante una vigencia. */
public class TarifaMembresia {

  // TODO(TRA-25): Mapear como entidad JPA con una restricción que impida vigencias superpuestas
  // para
  // el mismo nivel y relación UNSE.
  private UUID id;
  private UUID nivelMembresiaId;
  private RelacionUnse relacionUnse;
  private BigDecimal importeMensual;
  private LocalDate vigenciaDesde;
  private LocalDate vigenciaHasta;

  /** TODO(TRA-25): Agregar constructor, getters y reglas para importes positivos y rangos de
  * vigencia
  * válidos. Conservar tarifas anteriores para explicar importes históricos.*/
}
