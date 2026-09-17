package edu.unse.sera.socio.entity;

import edu.unse.sera.usuario.entity.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Datos específicos de socio que complementan a un usuario registrado. */
@Entity
@Table(name = "socios")
public class Socio {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(name = "relacion_unse", nullable = false)
  private RelacionUnse relacionUnse;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_verificacion_unse", nullable = false)
  private EstadoVerificacionUnse estadoVerificacionUnse;

  @Column(unique = true, name = "identificador_unse")
  private String identificadorUnse;



  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Socio() {  }

  public Socio(Usuario usuario, RelacionUnse relacionUnse,
              EstadoVerificacionUnse estadoVerificacionUnse, String identificadorUnse) {
    this.usuario = usuario;
    actualizarDatos(relacionUnse,estadoVerificacionUnse,identificadorUnse);
  }



  public UUID getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public RelacionUnse getRelacionUnse() {
    return relacionUnse;
  }

  public EstadoVerificacionUnse getEstadoVerificacionUnse() {
    return estadoVerificacionUnse;
  }

  public String getIdentificadorUnse() {
    return identificadorUnse;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void actualizarDatos(RelacionUnse relacionUnse,
    EstadoVerificacionUnse estadoVerificacionUnse, String identificadorUnse) {
    this.relacionUnse = relacionUnse;
    this.estadoVerificacionUnse = estadoVerificacionUnse;
    this.identificadorUnse = normalizarIdentificador(identificadorUnse);
  }

  public String normalizarIdentificador(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    return valor.trim().toLowerCase(Locale.ROOT);
  }

}
