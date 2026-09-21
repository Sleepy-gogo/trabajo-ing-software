package edu.unse.sera.usuario.entity;

import edu.unse.sera.socio.entity.Socio;
import io.github.thibaultmeyer.cuid.CUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
  @GeneratedValue(strategy = GenerationType.UUID)
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private RolUsuario rol;

  @Column(name = "qr_usuario", nullable = false, length = 18, unique = true, updatable = false)
  private String qrUsuario;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @OneToOne(mappedBy = "usuario")
  private Socio socio;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Usuario() {}

  /** Genera el QR dentro del dominio para que ningún cliente pueda elegirlo o reemplazarlo. */
  public Usuario(
      String nombreCompleto,
      String email,
      int dni,
      EstadoUsuario estadoCuenta,
      RolUsuario rol,
      String passwordHash) {
    this.estadoCuenta = Objects.requireNonNull(estadoCuenta);
    this.qrUsuario = generarQrUsuario();
    actualizarDatos(nombreCompleto, email, dni, rol);
    cambiarPasswordHash(passwordHash);
  }

  /** Mantiene las mismas reglas para los datos editables durante el alta y la actualización. */
  public void actualizarDatos(String nombreCompleto, String email, int dni, RolUsuario rol) {
    this.nombreCompleto = normalizarNombre(nombreCompleto);
    this.email = normalizarEmail(email);
    this.dni = validarDni(dni);
    this.rol = Objects.requireNonNull(rol, "El rol es obligatorio.");
  }

  /** Permite suspender o reactivar una cuenta sin borrar su historial. */
  public void cambiarEstado(EstadoUsuario estadoCuenta) {
    this.estadoCuenta = Objects.requireNonNull(estadoCuenta);
  }

  /** Recibe solo hashes para que la entidad nunca conserve una contraseña en texto plano. */
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

  public RolUsuario getRol() {
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

  private String generarQrUsuario() {
    return "SERA-U" + CUID.randomCUID2(12);
  }
}
