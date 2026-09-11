package edu.unse.sera.espacio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

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

  protected Espacio() {}

  public Espacio(String nombre, String descripcion) {
    actualizar(nombre, descripcion);
  }

  public void actualizar(String nombre, String descripcion) {
    String nombreNormalizado = normalizarNombre(nombre);
    String descripcionNormalizada = normalizarDescripcion(descripcion);
    this.nombre = nombreNormalizado;
    this.descripcion = descripcionNormalizada;
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
}
