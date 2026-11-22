package edu.unse.sera.actividad.persistence;

import edu.unse.sera.actividad.entity.RegistroActividad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroActividadRepository extends JpaRepository<RegistroActividad, UUID> {
  List<RegistroActividad> findTop8ByOrderByCreadoEnDesc();
}
