package edu.unse.sera.actividad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "registros_actividad")
public class RegistroActividad {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "usuario_id")
  private UUID usuarioId;

  @Column(length = 100)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TipoActividad tipo;

  @Column(length = 500)
  private String detalle;

  @Column(name = "creado_en", nullable = false)
  private OffsetDateTime creadoEn;

  protected RegistroActividad() {}

  public RegistroActividad(
      UUID usuarioId, String email, TipoActividad tipo, String detalle, OffsetDateTime creadoEn) {
    this.usuarioId = usuarioId;
    this.email = recortar(email, 100);
    this.tipo = Objects.requireNonNull(tipo, "El tipo de actividad es obligatorio.");
    this.detalle = recortar(detalle, 500);
    this.creadoEn = Objects.requireNonNull(creadoEn, "La fecha de la actividad es obligatoria.");
  }

  private static String recortar(String valor, int maximo) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    String limpio = valor.trim();
    return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  public String getEmail() {
    return email;
  }

  public TipoActividad getTipo() {
    return tipo;
  }

  public String getDetalle() {
    return detalle;
  }

  public OffsetDateTime getCreadoEn() {
    return creadoEn;
  }
}
