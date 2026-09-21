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

  protected NivelMembresia() {}

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

  public void actualizar(
      String nombre,
      String descripcion,
      Map<RelacionUnse, BigDecimal> precios,
      List<String> beneficios,
      boolean disponible) {
    if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
      throw new IllegalArgumentException(
          "El nombre del nivel es obligatorio y admite 100 caracteres.");
    }
    if (precios == null
        || precios.isEmpty()
        || precios.entrySet().stream()
            .anyMatch(
                e ->
                    e.getKey() == null
                        || e.getValue() == null
                        || e.getValue().signum() <= 0
                        || e.getValue().scale() > 2
                        || e.getValue().compareTo(new BigDecimal("99999999.99")) > 0)) {
      throw new IllegalArgumentException(
          "Indicá precios positivos de hasta 8 enteros y 2 decimales.");
    }
    if (beneficios == null
        || beneficios.isEmpty()
        || beneficios.stream().anyMatch(v -> v == null || v.isBlank() || v.length() > 255)) {
      throw new IllegalArgumentException("Indicá al menos un beneficio de hasta 255 caracteres.");
    }
    this.nombre = nombre.trim();
    this.descripcion = descripcion;
    this.preciosPorRelacion.clear();
    this.preciosPorRelacion.putAll(precios);
    this.beneficios = new java.util.ArrayList<>(beneficios);
    this.disponibleParaContratar = disponible;
  }

  public void deshabilitar() {
    disponibleParaContratar = false;
  }
}
