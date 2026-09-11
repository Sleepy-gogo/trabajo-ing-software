package edu.unse.sera.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "usuarios")
public class Usuario {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "nombre_completo", nullable = false, length = 200)
  private String nombreCompleto;

  @Column(nullable = false, length = 100, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private int dni;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_cuenta", nullable = false)
  private EstadoUsuario estadoCuenta;

  @Column(nullable = false, length = 50)
  private String rol;

  @Column(name = "qr_usuario", nullable = false, length = 100)
  private String qrUsuario;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Usuario() {}

  public Usuario(
      String nombreCompleto,
      String email,
      int dni,
      EstadoUsuario estadoCuenta,
      String rol,
      String qrUsuario,
      String passwordHash) {
    this.id = UUID.randomUUID();
    this.estadoCuenta = Objects.requireNonNull(estadoCuenta);
    actualizarDatos(nombreCompleto, email, dni, rol, qrUsuario);
    cambiarPasswordHash(passwordHash);
  }

  public void actualizarDatos(
      String nombreCompleto, String email, int dni, String rol, String qrUsuario) {
    this.nombreCompleto = normalizarNombre(nombreCompleto);
    this.email = normalizarEmail(email);
    this.dni = validarDni(dni);
    this.rol = normalizarAtributoObligatorio(rol, "El rol es obligatorio.");
    this.qrUsuario = normalizarAtributoObligatorio(qrUsuario, "El QR de usuario es obligatorio.");
  }

  public void cambiarEstado(EstadoUsuario estadoCuenta) {
    this.estadoCuenta = Objects.requireNonNull(estadoCuenta);
  }

  public void cambiarPasswordHash(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank()) {
      throw new IllegalArgumentException("El hash de contraseña es obligatorio.");
    }
    this.passwordHash = passwordHash;
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

  public String getRol() {
    return rol;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getQrUsuario() {
    return qrUsuario;
  }

  private String normalizarNombre(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("El nombre completo es obligatorio.");
    }
    return valor.trim();
  }

  private String normalizarEmail(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("El email es obligatorio.");
    }
    return valor.trim().toLowerCase(Locale.ROOT);
  }

  private int validarDni(int valor) {
    if (valor < 1 || valor > 99_999_999) {
      throw new IllegalArgumentException("El DNI debe tener entre 1 y 8 dígitos.");
    }
    return valor;
  }

  private String normalizarAtributoObligatorio(String valor, String mensaje) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException(mensaje);
    }
    return valor.trim();
  }
}
