package edu.unse.sera.espacio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "espacios")
public class Espacio {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 500)
  private String descripcion;

  protected Espacio() {}

  public Espacio(String nombre, String descripcion) {
    this.id = UUID.randomUUID();
    actualizar(nombre, descripcion);
  }

  public void actualizar(String nombre, String descripcion) {
    this.nombre = nombre.trim();
    this.descripcion = normalizarDescripcion(descripcion);
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

  private String normalizarDescripcion(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    return valor.trim();
  }
}
