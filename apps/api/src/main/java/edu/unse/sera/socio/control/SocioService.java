package edu.unse.sera.socio.control;

import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.socio.persistence.SocioRepository;
import edu.unse.sera.usuario.control.UsuarioService;
import edu.unse.sera.usuario.entity.Usuario;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso administrativos para socios y sus membresías. */
@Service
@Transactional
public class SocioService {

  private final SocioRepository socioRepository;
  private final UsuarioService usuarioService;

  public SocioService(SocioRepository socioRepository, UsuarioService usuarioService) {
    this.socioRepository = socioRepository;
    this.usuarioService = usuarioService;
  }

  public Socio buscar(UUID id) {
    return socioRepository.findById(id).orElseThrow(() -> new SocioNoEncontradoException(id));
  }

  public SocioDetalle registrar(
      UUID usuarioId,
      RelacionUnse relacionUnse,
      String identificadorUnse,
      NivelMembresia nivelMembresia) {

    Usuario usuario = usuarioService.buscar(usuarioId);
    // En caso de tomar NivelMembresia como su UUID.
    // Implementaría un findById en el repository de nivelmembresia
    Socio socio =
        new Socio(usuario, relacionUnse, EstadoVerificacionUnse.PENDIENTE, identificadorUnse);
    Membresia membresia = new Membresia(socio, nivelMembresia);
    socio.setMembresia(membresia);
    return toResponse(socio);
  }

  // consultar == obtener
  @Transactional(readOnly = true)
  public SocioDetalle consultar(UUID id) {
    Socio socio = buscar(id);
    return toResponse(socio);
  }

  // TODO(TRA-28): Buscar por nombre, DNI o email y aplicar los filtros solo cuando estén
  // presentes.
  @Transactional(readOnly = true)
  public List<SocioDetalle> listar(
      String criterio, EstadoMembresia estadoMembresia, RelacionUnse relacionUnse) {
    List<Socio> socios;
    if (criterio == null || criterio.isBlank()) {
      socios = socioRepository.findAllOrderedByUsuarioNombreCompleto();
    } else {
      String valor = criterio.trim();
      socios =
          usuarioService
              .convertirDni(valor)
              .flatMap(socioRepository::findByUsuarioDni)
              .map(List::of)
              .orElseGet(() -> socioRepository.buscarSociosPorNombreOEmail(valor, valor));
    }
    return socios.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public SocioDetalle consultarPorFiltros(
      String criterio, EstadoMembresia estadoMembresia, RelacionUnse relacionUnse) {
    return listar(criterio, estadoMembresia, relacionUnse).getFirst();
  }

  // TODO(TRA-27): Validar nivel y transiciones de estado antes de guardar. Registrar quién hizo el
  // cambio cuando el modelo de autenticación exponga al usuario responsable.
  public SocioDetalle actualizarDatosSocio(
      UUID socioId,
      RelacionUnse relacionUnse,
      EstadoVerificacionUnse estadoVerificacionUnse,
      String identificadorUnse,
      String motivo) {

    Socio socio = buscar(socioId);
    socio.actualizarDatos(relacionUnse, estadoVerificacionUnse, identificadorUnse);
    // TODO: Implementar registro de auditoría con el motivo
    return toResponse(socio);
  }

  public SocioDetalle actualizarMembresia(UUID id, NivelMembresia nivelMembresia) {
    Socio socio = buscar(id);
    Membresia membresia = socio.getMembresia();
    membresia.setNivelMembresia(nivelMembresia);

    return toResponse(socio);
  }

  private SocioDetalle toResponse(Socio socio) {
    Usuario usuario = socio.getUsuario();
    Membresia membresia = socio.getMembresia();
    NivelMembresia nivelMembresia = membresia.getNivelMembresia();
    return new SocioDetalle(
        socio.getId(),
        usuario.getId(),
        usuario.getNombreCompleto(),
        usuario.getEmail(),
        usuario.getDni(),
        socio.getRelacionUnse(),
        socio.getEstadoVerificacionUnse(),
        socio.getIdentificadorUnse(),
        membresia.getId(),
        nivelMembresia.getId(),
        nivelMembresia.getNombre(),
        membresia.getEstado(),
        membresia.getProximoVencimiento());
  }
}
