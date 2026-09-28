package edu.unse.sera.pagos.persistence;

import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.Pago;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

  Optional<Pago> findByReservaId(UUID reservaId);

  List<Pago> findAllByUsuarioId(UUID usuarioId);

  @Query(
      "SELECT p FROM Pago p WHERE (:usuarioId IS NULL OR p.usuario.id = :usuarioId)"
          + " AND (:estado IS NULL OR p.estado = :estado)"
          + " ORDER BY p.createdAt DESC, p.id DESC")
  Page<Pago> buscarHistorial(
      @Param("usuarioId") UUID usuarioId, @Param("estado") EstadoPago estado, Pageable pagina);

  Optional<Pago> findByUsuarioIdAndClaveSolicitud(UUID usuarioId, UUID claveSolicitud);

  Optional<Pago> findByMembresiaIdAndContratacionIdAndEstado(
      UUID membresiaId, UUID contratacionId, EstadoPago estado);

  List<Pago> findAllByMembresiaId(UUID membresiaId);

  boolean existsByMembresiaIdAndEstado(UUID membresiaId, EstadoPago estado);

  Optional<Pago> findByMercadoPagoPaymentId(String paymentId);

  Optional<Pago> findByMercadoPagoFacturaIdAndEstado(Long facturaId, EstadoPago estado);

  List<Pago> findAllByEstadoAndCreatedAtBefore(EstadoPago estado, OffsetDateTime fecha);
}
