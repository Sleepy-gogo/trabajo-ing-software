package edu.unse.sera.encuesta.persistence;

import edu.unse.sera.encuesta.entity.EnvioEncuesta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvioEncuestaRepository extends JpaRepository<EnvioEncuesta, UUID> {
  Optional<EnvioEncuesta> findByEncuestaIdAndReservaId(UUID encuestaId, UUID reservaId);

  List<EnvioEncuesta> findAllByEncuestaIdOrderByEnviadaEnDesc(UUID encuestaId);
}
