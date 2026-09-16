package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.DiaSemana;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadDetalle(DiaSemana diaSemana, LocalTime horaDesde, LocalTime horaHasta, UUID espacioId) {

}
