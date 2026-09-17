package edu.unse.sera.membresia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Membresía contratada por un socio. Los importes pertenecen a las cuotas y pagos. */
@Entity
@Table(name = "membresias")
public class Membresia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "socio_id", nullable = false)
  private UUID socioId;

  @Column(name = "nivel_membresia_id", nullable = false)
  private UUID nivelMembresiaId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoMembresia estado;

  @Column(nullable = false, name = "fecha_alta")
  private LocalDate fechaAlta;

  @Column(name = "fecha_baja")
  private LocalDate fechaBaja;

  @Column(name = "proximo_vencimiento", nullable = false)
  private LocalDate proximoVencimiento;

  private OffsetDateTime creadoEn;
  private OffsetDateTime actualizadoEn;

  // TODO(TRA-26): Mapear como entidad JPA y agregar la migración una vez que se integren las
  // migraciones de Usuario. No fijar ahora un número de versión que pueda colisionar con esa rama.

  // TODO(TRA-27): Agregar comportamiento para activar, suspender, vencer y cancelar. El precio
  // del siguiente período se resuelve desde NivelMembresia y se copia a la Cuota generada.
}
