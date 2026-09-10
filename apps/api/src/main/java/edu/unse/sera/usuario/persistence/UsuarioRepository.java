package edu.unse.sera.usuario.persistence;

import edu.unse.sera.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  Optional<Usuario> findByEmail(String email);

  Optional<Usuario> findByUsername(String username);

  boolean existsByEmail(String email);
}
