package edu.unse.sera.reporte.persistence;

import edu.unse.sera.reporte.entity.Informe;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InformeRepository extends JpaRepository<Informe, UUID> {}
