package edu.unse.sera.usuario.persistence;

import edu.unse.sera.usuario.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  Optional<Usuario> findByEmailIgnoreCase(String email);

  List<Usuario> findAllByOrderByNombreCompletoAsc();

  List<Usuario> findByNombreCompletoContainingIgnoreCaseOrderByNombreCompletoAsc(
      String nombreCompleto);

  Optional<Usuario> findByDni(int dni);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

  boolean existsByDni(int dni);

  boolean existsByDniAndIdNot(int dni, UUID id);
}
