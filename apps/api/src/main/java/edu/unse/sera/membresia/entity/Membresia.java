package edu.unse.sera.membresia.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Membresía contratada por un socio. */
public class Membresia {

  // TODO(TRA-26): Mapear como entidad JPA y agregar la migración una vez que se integren las
  // migraciones de Usuario. No fijar ahora un número de versión que pueda colisionar con esa rama.
  private UUID id;
  private UUID socioId;
  private UUID nivelMembresiaId;
  private EstadoMembresia estado;
  private LocalDate fechaAlta;
  private LocalDate fechaBaja;
  private LocalDate proximoVencimiento;
  private BigDecimal cargoMensual;
  private OffsetDateTime creadoEn;
  private OffsetDateTime actualizadoEn;

  // TODO(TRA-27): Agregar comportamiento para activar, suspender, vencer y cancelar. Las cuotas y
  // resultados de pago pertenecen al módulo de pagos; Membresia solo reacciona a esos resultados.
}
