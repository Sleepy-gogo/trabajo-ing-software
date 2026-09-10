package edu.unse.sera.usuario.persistence;

import edu.unse.sera.usuario.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  Optional<Usuario> findByEmail(String email);

  List<Usuario> findByNombreCompletoContainingIgnoreCase(String nombreCompleto);

  Optional<Usuario> findByDni(int dni);

  Optional<Usuario> findByQrCode(String qrCode);

  boolean existsByEmail(String email);
}
