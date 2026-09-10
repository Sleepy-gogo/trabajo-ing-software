package edu.unse.sera.usuario.control;


import java.util.UUID;

public record UsuarioDetalle(UUID id, String nombreCompleto, String email, int dni) {}
