package edu.unse.sera.usuario.boundary.dto;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String nombreCompleto,
    String email,
    int dni,
    RolUsuario rol,
    String qrUsuario,
    EstadoUsuario estadoCuenta,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn) {
  public static UsuarioResponse from(edu.unse.sera.usuario.control.UsuarioDetalle usuario) {
    return new UsuarioResponse(
        usuario.id(),
        usuario.nombreCompleto(),
        usuario.email(),
        usuario.dni(),
        usuario.rol(),
        usuario.qrUsuario(),
        usuario.estadoCuenta(),
        usuario.creadoEn(),
        usuario.actualizadoEn());
  }
}
