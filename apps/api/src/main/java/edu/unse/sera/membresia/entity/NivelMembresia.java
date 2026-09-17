package edu.unse.sera.membresia.entity;

import edu.unse.sera.socio.entity.RelacionUnse;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Plan contratable con precios por relación UNSE, beneficios y condiciones. */
@Entity
@Table(name = "nivel_membresias")
public class NivelMembresia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(unique = true, nullable = false)
  private String nombre;

  @Column(length = 500)
  private String descripcion;

  @ElementCollection
  @CollectionTable(
      name = "nivel_membresia_precio_relacion",
      joinColumns = @JoinColumn(name = "nivel_membresia_id"))
  @MapKeyEnumerated(EnumType.STRING)
  @MapKeyColumn(name = "relacion")
  @Column(name = "precio")
  private Map<RelacionUnse, BigDecimal> preciosPorRelacion = new EnumMap<>(RelacionUnse.class);

  @ElementCollection
  @CollectionTable(
      name = "nivel_membresia_beneficios",
      joinColumns = @JoinColumn(name = "nivel_membresia_id"))
  @Column(name = "beneficio")
  private List<String> beneficios;

  @Column(name = "disponible")
  private boolean disponibleParaContratar;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public NivelMembresia(String nombre, String descripcion) {
    this.nombre = nombre;
    this.descripcion = descripcion;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isDisponibleParaContratar() {
    return disponibleParaContratar;
  }

  public List<String> getBeneficios() {
    return beneficios;
  }

  public Map<RelacionUnse, BigDecimal> getPreciosPorRelacion() {
    return preciosPorRelacion;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getNombre() {
    return nombre;
  }

  public UUID getId() {
    return id;
  }

  // TODO(TRA-25): Agregar validaciones. Los importes deben ser positivos y
  // un nivel histórico puede quedar inactivo sin borrar membresías anteriores.
}
