package edu.unse.sera.usuario.control;


import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import java.util.UUID;

public record UsuarioDetalle(UUID id, String nombreCompleto, String email, int dni,
                             RolUsuario rolUsuario, String qrCode, EstadoUsuario estadoUsuario) {

}
