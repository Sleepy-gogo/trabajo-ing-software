package edu.unse.sera.disponibilidad.boundary.dto;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadResponse(
    UUID id, UUID espacioId, DiaSemana diaSemana, LocalTime horaDesde, LocalTime horaHasta) {}
