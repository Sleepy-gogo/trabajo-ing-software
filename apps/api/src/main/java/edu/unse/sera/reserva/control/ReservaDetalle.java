package edu.unse.sera.reserva.control;

import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservaDetalle(
    UUID id,
    UUID usuarioId,
    String titular,
    UUID espacioId,
    String espacioNombre,
    LocalDate fecha,
    LocalTime desde,
    LocalTime hasta,
    int personas,
    BigDecimal tarifaHora,
    String relacionAplicada,
    BigDecimal total,
    BigDecimal creditoAplicado,
    BigDecimal saldoTicket,
    String estado,
    String codigo,
    OffsetDateTime venceEn,
    UUID pagoId,
    EstadoPago estadoPago,
    MedioPago medioPago,
    String checkoutUrl,
    boolean requiereRevision,
    boolean cancelable,
    OffsetDateTime consumidaEn) {}
