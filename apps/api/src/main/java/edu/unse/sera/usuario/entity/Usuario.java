package edu.unse.sera.usuario.entity;

import io.github.thibaultmeyer.cuid.CUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "usuarios")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "nombre_completo", nullable = false, length = 200)
  private String nombreCompleto;

  @Column(nullable = false, length = 100, unique = true)
  private String email;

  @Column(nullable = false, length = 8, unique = true)
  private int dni;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_cuenta", nullable = false)
  private EstadoUsuario estadoCuenta;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol_usuario", nullable = false)
  private RolUsuario rolUsuario;

  @Column(name = "qr_code", nullable = false, unique = true)
  private String qrCode;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Usuario() {
  }

  public Usuario(String nombreCompleto, String email, int dni, EstadoUsuario estadoCuenta,
    RolUsuario rol, String passwordHash) {
    final CUID preCuid = CUID.randomCUID2(12);

    this.nombreCompleto = nombreCompleto;
    this.email = email;
    this.dni = dni;
    this.estadoCuenta = estadoCuenta;
    this.passwordHash = passwordHash;
    this.qrCode = "SERA-U" + preCuid.toString();
    this.rolUsuario = rol;
  }


  public String getPasswordHash() {
    return passwordHash;
  }

  public void actualizar(String nombreCompleto, String email, int dni, EstadoUsuario estadoCuenta) {
    this.nombreCompleto = normalizarNombre(nombreCompleto);
    this.email = email;
    this.dni = dni;
    this.estadoCuenta = estadoCuenta;
  }

  public UUID getId() {
    return id;
  }

  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public String getEmail() {
    return email;
  }

  public EstadoUsuario getEstadoCuenta() {
    return estadoCuenta;
  }

  public int getDni() {
    return dni;
  }

  public void setNombreCompleto(String nombreCompleto) {
    this.nombreCompleto = nombreCompleto;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setDni(int dni) {
    this.dni = dni;
  }

  public void setEstadoCuenta(EstadoUsuario estadoCuenta) {
    this.estadoCuenta = estadoCuenta;
  }

  public RolUsuario getRolUsuario() {
    return rolUsuario;
  }

  public void setRolUsuario(RolUsuario rolUsuario) {
    this.rolUsuario = rolUsuario;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getQrCode() {
    return qrCode;
  }

  public void setQrCode(String qrCode) {
    this.qrCode = qrCode;
  }

  private String normalizarNombre(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    return valor.trim();
  }
}
