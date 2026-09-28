package edu.unse.sera.reserva.persistence;

import edu.unse.sera.reserva.entity.Reserva;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
  Optional<Reserva> findByCodigo(String codigo);

  @Query("select r.usuario.id from Reserva r where r.id = :id")
  Optional<UUID> titular(UUID id);

  Optional<Reserva> findByUsuarioIdAndClaveSolicitud(UUID usuarioId, UUID claveSolicitud);

  @Query(
      "select r from Reserva r where (:usuario is null or r.usuario.id = :usuario) order by r.fecha desc, r.desde desc")
  List<Reserva> listar(UUID usuario);

  @Query(
      "select r from Reserva r where r.espacio.id = :espacio and r.fecha = :fecha"
          + " and r.estado in ('PENDIENTE_PAGO', 'CONFIRMADA') order by r.desde")
  List<Reserva> ocupadas(UUID espacio, LocalDate fecha);

  @Query("select r.id from Reserva r where r.estado = 'PENDIENTE_PAGO' and r.venceEn <= :ahora")
  List<UUID> vencidas(OffsetDateTime ahora);

  @Query(
      "select count(r) > 0 from Reserva r where r.espacio.id = :espacio and r.fecha >= :fecha"
          + " and r.estado in ('PENDIENTE_PAGO', 'CONFIRMADA')")
  boolean tieneReservasFuturas(UUID espacio, LocalDate fecha);
}
