package edu.unse.sera.pagos.persistence;

import edu.unse.sera.pagos.entity.SuscripcionMercadoPago;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscripcionMercadoPagoRepository
    extends JpaRepository<SuscripcionMercadoPago, UUID> {
  Optional<SuscripcionMercadoPago> findByPreapprovalId(String preapprovalId);

  Optional<SuscripcionMercadoPago> findByPagoInicialId(UUID pagoInicialId);

  boolean existsByPagoInicialIdAndEstadoNot(UUID pagoInicialId, String estado);

  List<SuscripcionMercadoPago> findAllByMembresiaIdOrderByCreatedAtDesc(UUID membresiaId);
}
