package edu.unse.sera.pagos.persistence;

import edu.unse.sera.pagos.entity.EstadoPago;
import edu.unse.sera.pagos.entity.Pago;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

  List<Pago> findAllByUsuarioId(UUID usuarioId);

  List<Pago> findAllByMembresiaId(UUID membresiaId);

  boolean existsByMembresiaIdAndEstado(UUID membresiaId, EstadoPago estado);

  List<Pago> findAllByEstadoAndCreatedAtBefore(EstadoPago estado, OffsetDateTime fecha);
}
