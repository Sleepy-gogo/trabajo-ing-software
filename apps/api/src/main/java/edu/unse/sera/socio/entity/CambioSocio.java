package edu.unse.sera.socio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cambios_socios")
public class CambioSocio {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID socioId;

  @Column(nullable = false)
  private UUID responsableId;

  @Column(nullable = false, length = 500)
  private String motivo;

  @Column(nullable = false, length = 2000)
  private String detalle;

  @Column(nullable = false)
  private OffsetDateTime fecha;

  protected CambioSocio() {}

  public CambioSocio(UUID socioId, UUID responsableId, String motivo, String detalle) {
    if (motivo == null || motivo.isBlank() || motivo.length() > 500) {
      throw new IllegalArgumentException("Indicá un motivo de hasta 500 caracteres.");
    }
    this.socioId = socioId;
    this.responsableId = responsableId;
    this.motivo = motivo.trim();
    this.detalle = detalle;
    this.fecha = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getResponsableId() {
    return responsableId;
  }

  public String getMotivo() {
    return motivo;
  }

  public String getDetalle() {
    return detalle;
  }

  public OffsetDateTime getFecha() {
    return fecha;
  }
}
