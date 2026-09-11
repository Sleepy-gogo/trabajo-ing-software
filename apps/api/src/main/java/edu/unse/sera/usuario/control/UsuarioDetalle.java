package edu.unse.sera.usuario.control;

import edu.unse.sera.usuario.entity.EstadoUsuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioDetalle(
    UUID id,
    String nombreCompleto,
    String email,
    int dni,
    String rol,
    String qrUsuario,
    EstadoUsuario estadoCuenta,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn) {}
