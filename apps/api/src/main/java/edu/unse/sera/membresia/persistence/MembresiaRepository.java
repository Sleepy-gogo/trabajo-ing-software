package edu.unse.sera.membresia.persistence;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a persistencia de membresías contratadas. */
public interface MembresiaRepository extends JpaRepository<Membresia, UUID> {

  // 1. buscarMembresiaActiva(idUsuario)
  @Query(
      "SELECT m FROM Membresia m JOIN FETCH m.socio s JOIN FETCH m.nivelMembresia nm WHERE"
          + " s.usuario.id = :idUsuario AND m.estado = :estadoActiva")
  Optional<Membresia> buscarMembresiaPorUsuarioIdPorEstado(
      @Param("idUsuario") UUID idUsuario, @Param("estadoActiva") EstadoMembresia estadoActiva);

  // 2. buscarMembresiaUsuario(idUsuario)
  @Query(
      "SELECT m FROM Membresia m JOIN FETCH m.socio s JOIN FETCH m.nivelMembresia nm WHERE s.usuario.id = :idUsuario")
  Optional<Membresia> buscarMembresiaPorUsuarioId(@Param("idUsuario") UUID idUsuario);

  // 3. buscarMembresiaSocio(idSocio)
  @Query(
      "SELECT m FROM Membresia m JOIN FETCH m.socio s JOIN FETCH m.nivelMembresia nm WHERE s.id = :idSocio")
  Optional<Membresia> buscarMembresiaPorSocioId(@Param("idSocio") UUID idSocio);

  // 4. verificarEstadoMembresia(idSocio)
  @Query("SELECT m.estado FROM Membresia m WHERE m.socio.id = :idSocio")
  Optional<EstadoMembresia> obtenerEstadoMembresiaPorSocioId(@Param("idSocio") UUID idSocio);

  // 5. verificarMembresiaActivaOPendiente(idSocio)
  boolean existsBySocioIdAndEstadoIn(UUID socioId, List<EstadoMembresia> estados);
}
