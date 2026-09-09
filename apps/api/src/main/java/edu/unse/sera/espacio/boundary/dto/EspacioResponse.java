package edu.unse.sera.espacio.boundary.dto;

import java.util.UUID;

public record EspacioResponse(UUID id, String nombre, String descripcion) {}
