package edu.unse.sera.espacio.persistence;

import edu.unse.sera.espacio.entity.Espacio;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacioRepository extends JpaRepository<Espacio, UUID> {}
