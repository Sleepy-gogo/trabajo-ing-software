package edu.unse.sera.membresia.entity;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Plan contratable con precios por relación UNSE, beneficios y condiciones. */
public class NivelMembresia {

  // TODO(TRA-25): Mapear como entidad JPA. Los precios pueden persistirse como una colección de
  // elementos con relación UNSE como clave, sin crear una entidad de tarifa.
  // Definir la relación con servicios cuando exista ese modelo.
  private UUID id;
  private String nombre;
  private String descripcion;
  private Map<RelacionUnse, BigDecimal> preciosPorRelacion = new EnumMap<>(RelacionUnse.class);
  private List<String> beneficios;
  private List<String> serviciosIncluidos;
  private List<String> condiciones;
  private boolean disponibleParaContratar;

  // TODO(TRA-25): Agregar constructor, getters y validaciones. Los importes deben ser positivos y
  // un nivel histórico puede quedar inactivo sin borrar membresías anteriores.
}
