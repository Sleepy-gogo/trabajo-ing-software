package edu.unse.sera.espacio.entity;

import edu.unse.sera.disponibilidad.entity.Disponibilidad;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "espacios")
public class Espacio {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 500)
  private String descripcion;

  @Column(nullable = false)
  private int capacidad;

  @Column(name = "tarifa_hora", nullable = false, precision = 12, scale = 2)
  private BigDecimal tarifaHora;

  @Column(nullable = false, length = 100)
  private String tipo;

  @Column(name = "ruta_imagen")
  private String rutaImagen;

  @OneToMany(mappedBy = "espacio", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Disponibilidad> disponibilidad = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(nullable = false)
  private EstadoEspacio estado = EstadoEspacio.HABILITADO;

  @jakarta.persistence.ElementCollection
  @jakarta.persistence.CollectionTable(
      name = "espacio_tarifas",
      joinColumns = @jakarta.persistence.JoinColumn(name = "espacio_id"))
  @jakarta.persistence.MapKeyEnumerated(jakarta.persistence.EnumType.STRING)
  @jakarta.persistence.MapKeyColumn(name = "relacion")
  @Column(name = "importe", precision = 12, scale = 2)
  private java.util.Map<edu.unse.sera.socio.entity.RelacionUnse, BigDecimal> tarifas =
      new java.util.EnumMap<>(edu.unse.sera.socio.entity.RelacionUnse.class);

  public EstadoEspacio getEstado() {
    return estado;
  }

  public void cambiarEstado(EstadoEspacio estado) {
    this.estado = Objects.requireNonNull(estado);
  }

  public java.util.Map<edu.unse.sera.socio.entity.RelacionUnse, BigDecimal> getTarifas() {
    return java.util.Map.copyOf(tarifas);
  }

  public void configurarTarifas(
      java.util.Map<edu.unse.sera.socio.entity.RelacionUnse, BigDecimal> nuevas) {
    if (nuevas == null
        || nuevas.entrySet().stream()
            .anyMatch(
                e ->
                    e.getKey() == null
                        || e.getValue() == null
                        || e.getValue().signum() < 0
                        || e.getValue().scale() > 2
                        || e.getValue().compareTo(new BigDecimal("9999999999.99")) > 0)) {
      throw new IllegalArgumentException(
          "Las tarifas deben ser importes no negativos de hasta 10 enteros y 2 decimales.");
    }
    tarifas.clear();
    tarifas.putAll(nuevas);
  }

  public BigDecimal tarifaPara(edu.unse.sera.socio.entity.RelacionUnse relacion) {
    return tarifas.getOrDefault(relacion, tarifaHora);
  }

  protected Espacio() {}

  public Espacio(
      String nombre,
      String descripcion,
      int capacidad,
      BigDecimal tarifaHora,
      String tipo,
      String rutaImagen) {
    actualizar(nombre, descripcion, capacidad, tarifaHora, tipo, rutaImagen);
  }

  public void actualizar(
      String nombre,
      String descripcion,
      int capacidad,
      BigDecimal tarifaHora,
      String tipo,
      String rutaImagen) {
    String nombreNormalizado = normalizarNombre(nombre);
    String descripcionNormalizada = normalizarDescripcion(descripcion);
    String tipoNormalizado = normalizarTipo(tipo);
    if (capacidad <= 0) {
      throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");
    }
    BigDecimal tarifaNormalizada =
        Objects.requireNonNull(tarifaHora, "La tarifa por hora es obligatoria.");
    if (tarifaNormalizada.signum() < 0) {
      throw new IllegalArgumentException("La tarifa por hora no puede ser negativa.");
    }
    this.nombre = nombreNormalizado;
    this.descripcion = descripcionNormalizada;
    this.capacidad = capacidad;
    this.tarifaHora = tarifaNormalizada;
    this.tipo = tipoNormalizado;
    this.rutaImagen = normalizarRutaImagen(rutaImagen);
  }

  public UUID getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }

  private String normalizarNombre(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("El nombre es obligatorio.");
    }

    String nombreNormalizado = valor.trim();
    if (nombreNormalizado.length() > 100) {
      throw new IllegalArgumentException("El nombre no puede superar los 100 caracteres.");
    }
    return nombreNormalizado;
  }

  private String normalizarDescripcion(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }

    String descripcionNormalizada = valor.trim();
    if (descripcionNormalizada.length() > 500) {
      throw new IllegalArgumentException("La descripción no puede superar los 500 caracteres.");
    }
    return descripcionNormalizada;
  }

  private String normalizarTipo(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("El tipo es obligatorio.");
    }
    String tipoNormalizado = valor.trim();
    if (tipoNormalizado.length() > 100) {
      throw new IllegalArgumentException("El tipo no puede superar los 100 caracteres.");
    }
    return tipoNormalizado;
  }

  private String normalizarRutaImagen(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    String rutaNormalizada = valor.trim();
    if (rutaNormalizada.length() > 255) {
      throw new IllegalArgumentException("La ruta de imagen no puede superar los 255 caracteres.");
    }
    return rutaNormalizada;
  }

  public String getTipo() {
    return tipo;
  }

  public void agregarDisponibilidad(Disponibilidad nuevaDisponibilidad) {
    Objects.requireNonNull(nuevaDisponibilidad, "La disponibilidad es obligatoria.");
    if (nuevaDisponibilidad.getEspacio() != this) {
      throw new IllegalArgumentException("La disponibilidad pertenece a otro espacio.");
    }
    if (disponibilidad.stream()
        .anyMatch(
            existente ->
                existente.getDiaSemana().equals(nuevaDisponibilidad.getDiaSemana())
                    && existente.getHoraDesde().isBefore(nuevaDisponibilidad.getHoraHasta())
                    && existente.getHoraHasta().isAfter(nuevaDisponibilidad.getHoraDesde()))) {
      throw new IllegalArgumentException("El rango horario se superpone con otro existente.");
    }
    disponibilidad.add(nuevaDisponibilidad);
  }

  public BigDecimal getTarifaHora() {
    return tarifaHora;
  }

  public List<Disponibilidad> getDisponibilidad() {
    return List.copyOf(disponibilidad);
  }

  public int getCapacidad() {
    return capacidad;
  }

  public String getRutaImagen() {
    return rutaImagen;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
