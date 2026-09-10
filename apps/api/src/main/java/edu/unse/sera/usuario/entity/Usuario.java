package edu.unse.sera.usuario.entity;

import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String nombreCompleto;

  @Column(nullable = false, length = 100, unique = true)
  private String email;

  @Column(nullable = false, length = 8, unique = true)
  private int dni;

  @Column(nullable = false, length = 100)
  private EstadoUsuario estadoCuenta;

  @Column(nullable = false, length = 255)
  private String passwordHash;

  protected Usuario() {}

  public Usuario(String nombreCompleto, String email, int dni, EstadoUsuario estadoCuenta, String passwordHash) {

    this.nombreCompleto = nombreCompleto;
    this.email = email;
    this.dni = dni;
    this.estadoCuenta = estadoCuenta;
    this.passwordHash=passwordHash;
  }

  public boolean checkPass(String insPass) {
    return passwordHash.compareTo(insPass)==0;
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

  private String normalizarNombre(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    return valor.trim();
  }
}
