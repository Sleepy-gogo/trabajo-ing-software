package edu.unse.sera.pagos.persistence;

import edu.unse.sera.pagos.entity.Pago;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

  List<Pago> findAllByUsuarioId(UUID usuarioId);

  List<Pago> findAllByMembresiaId(UUID membresiaId);

}
