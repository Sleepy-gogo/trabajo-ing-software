package edu.unse.sera.socio.persistence;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocioRepository extends JpaRepository<Socio, UUID> {
  @EntityGraph(attributePaths = {"usuario", "membresia", "membresia.nivelMembresia"})
  Optional<Socio> findByUsuarioId(UUID usuarioId);

  @Override
  @EntityGraph(attributePaths = {"usuario", "membresia", "membresia.nivelMembresia"})
  Optional<Socio> findById(UUID id);

  @Query(
      """
   select s from Socio s join fetch s.usuario u
   left join fetch s.membresia m left join fetch m.nivelMembresia n
   where (:relacion is null or s.relacionUnse = :relacion)
   and (:estado is null or m.estado = :estado)
   and (:criterio = '' or lower(u.nombreCompleto) like concat('%', :criterio, '%')
      or lower(u.email) like concat('%', :criterio, '%') or cast(u.dni as string) = :criterio)
   order by u.nombreCompleto
   """)
  List<Socio> buscar(
      @Param("criterio") String criterio,
      @Param("estado") EstadoMembresia estado,
      @Param("relacion") RelacionUnse relacion);
}
