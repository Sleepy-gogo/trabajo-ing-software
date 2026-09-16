package edu.unse.sera.membresia.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Plan contratable con precio, beneficios, servicios y condiciones. */
public class NivelMembresia {

  // TODO(TRA-25): Mapear como entidad JPA. Las tarifas por relación UNSE viven en TarifaMembresia.
  // Definir la relación con servicios cuando exista ese modelo.
  private UUID id;
  private String nombre;
  private String descripcion;
  private BigDecimal importeMensualBase;
  private List<String> beneficios;
  private List<String> serviciosIncluidos;
  private List<String> condiciones;
  private boolean disponibleParaContratar;

  // TODO(TRA-25): Agregar constructor, getters y validaciones. Los importes deben ser positivos y
  // un nivel histórico puede quedar inactivo sin borrar membresías anteriores.
}
