package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadDetalle(
    UUID id, UUID espacioId, DiaSemana diaSemana, LocalTime horaDesde, LocalTime horaHasta) {}
