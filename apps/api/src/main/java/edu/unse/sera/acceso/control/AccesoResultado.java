package edu.unse.sera.acceso.control;

import java.time.LocalDate;
import java.time.LocalTime;

public record AccesoResultado(
    boolean autorizado,
    String motivo,
    String tipo,
    String titular,
    String espacio,
    LocalDate fecha,
    LocalTime desde,
    LocalTime hasta,
    java.time.OffsetDateTime consumidaEn) {}
