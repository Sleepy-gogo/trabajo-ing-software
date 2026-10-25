package edu.unse.sera.usuario.persistence;

import edu.unse.sera.usuario.entity.Usuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
  Optional<Usuario> findByQrUsuario(String qrUsuario);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from Usuario u where u.id = :id")
  Optional<Usuario> bloquearPorId(UUID id);

  Optional<Usuario> findByEmailIgnoreCase(String email);

  List<Usuario> findAllByOrderByNombreCompletoAsc();

  List<Usuario>
      findByNombreCompletoContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNombreCompletoAsc(
          String nombreCompleto, String email);

  Optional<Usuario> findByDni(int dni);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

  boolean existsByDni(int dni);

  boolean existsByDniAndIdNot(int dni, UUID id);
}
