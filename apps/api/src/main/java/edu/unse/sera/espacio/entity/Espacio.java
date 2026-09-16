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
import java.util.Optional;
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
    String tipoNormalizado = normalizarNombre(tipo);
    this.tarifaHora = tarifaHora;
    this.nombre = nombreNormalizado;
    this.descripcion = descripcionNormalizada;
    this.capacidad = capacidad;
    this.tipo = tipoNormalizado;
    this.rutaImagen = rutaImagen;
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

  public String getTipo() {
    return tipo;
  }

  public void agregarDisponibilidad(Disponibilidad nuevaDisponibilidad) {
    if (disponibilidad.stream()
        .noneMatch(d -> d.getDiaSemana().equals(nuevaDisponibilidad.getDiaSemana()))) {

      disponibilidad.add(nuevaDisponibilidad);
    }
  }

  public BigDecimal getTarifaHora() {
    return tarifaHora;
  }

  public List<Disponibilidad> getDisponibilidad() {
    return disponibilidad;
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

  public void actualizarDisponibilidad(Disponibilidad nuevaDisponibilidad) {
    Optional<Disponibilidad> resultado =
        disponibilidad.stream()
            .filter(d -> d.getDiaSemana().equals(nuevaDisponibilidad.getDiaSemana()))
            .findFirst();

    resultado.ifPresentOrElse(
        (viejaDisp) ->
            viejaDisp.actualizarDatos(
                nuevaDisponibilidad.getHoraDesde(), nuevaDisponibilidad.getHoraHasta()),
        () -> agregarDisponibilidad(nuevaDisponibilidad));
  }
}
