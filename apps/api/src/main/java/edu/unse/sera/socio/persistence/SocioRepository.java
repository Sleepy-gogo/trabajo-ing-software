package edu.unse.sera.socio.persistence;

import edu.unse.sera.socio.entity.Socio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a persistencia de socios. */
public interface SocioRepository extends JpaRepository<Socio, UUID> {
  // TODO(TRA-26): Extender JpaRepository<Socio, UUID> cuando Socio tenga mapeo JPA y migración.
  // Agregar únicamente las consultas que necesiten SocioService; evitar una capa DAO adicional.

  Optional<Socio> findByUsuarioId(UUID usuarioId);

  @Query("SELECT s FROM Socio s JOIN FETCH s.usuario u ORDER BY u.nombreCompleto ASC")
  List<Socio> findAllOrderedByUsuarioNombreCompleto();

  @Query(
      """
        SELECT s FROM Socio s
        JOIN FETCH s.usuario u
        WHERE LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombreCompleto, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))
        ORDER BY u.nombreCompleto ASC
    """)
  List<Socio> buscarSociosPorNombreOEmail(
      @Param("nombreCompleto") String nombreCompleto, @Param("email") String email);

  Optional<Socio> findByUsuarioDni(int dni);

  /*
   *+buscarSocioPorCriterio(criterio)
   *+buscarSociosPorFiltros(filtros)
   */
}
