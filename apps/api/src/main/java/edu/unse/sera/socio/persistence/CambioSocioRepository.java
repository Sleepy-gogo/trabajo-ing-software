package edu.unse.sera.socio.persistence;

import edu.unse.sera.socio.entity.CambioSocio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioSocioRepository extends JpaRepository<CambioSocio, UUID> {
  List<CambioSocio> findAllBySocioIdOrderByFechaDesc(UUID socioId);
}
