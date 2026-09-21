package edu.unse.sera.socio.control;

import edu.unse.sera.membresia.control.MembresiaDetalle;
import edu.unse.sera.membresia.control.MembresiaNoEncontradaException;
import edu.unse.sera.membresia.entity.EstadoMembresia;
import edu.unse.sera.membresia.entity.Membresia;
import edu.unse.sera.membresia.entity.NivelMembresia;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.membresia.persistence.NivelMembresiaRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.socio.entity.CambioSocio;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.entity.Socio;
import edu.unse.sera.socio.persistence.CambioSocioRepository;
import edu.unse.sera.socio.persistence.SocioRepository;
import edu.unse.sera.usuario.control.UsuarioService;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SocioService {
  private final SocioRepository socios;
  private final MembresiaRepository membresias;
  private final NivelMembresiaRepository niveles;
  private final CambioSocioRepository cambios;
  private final UsuarioService usuarios;

  public SocioService(
      SocioRepository socios,
      MembresiaRepository membresias,
      NivelMembresiaRepository niveles,
      CambioSocioRepository cambios,
      UsuarioService usuarios) {
    this.socios = socios;
    this.membresias = membresias;
    this.niveles = niveles;
    this.cambios = cambios;
    this.usuarios = usuarios;
  }

  private Socio buscar(UUID id) {
    return socios.findById(id).orElseThrow(() -> new SocioNoEncontradoException(id));
  }

  private void autorizar(Socio socio, UUID actor) {
    if (!socio.getUsuario().getId().equals(actor)
        && usuarios.buscar(actor).getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
  }

  private NivelMembresia nivel(UUID id, RelacionUnse relacion) {
    NivelMembresia n = niveles.findById(id).orElseThrow(MembresiaNoEncontradaException::new);
    if (!n.isDisponibleParaContratar() || !n.getPreciosPorRelacion().containsKey(relacion)) {
      throw new IllegalArgumentException(
          "El nivel no tiene una tarifa disponible para esa relación con la UNSE.");
    }
    return n;
  }

  public SocioDetalle registrar(
      UUID usuarioId, RelacionUnse relacion, String identificador, UUID nivelId, UUID actor) {
    if (!usuarioId.equals(actor) && usuarios.buscar(actor).getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
    var usuario = usuarios.buscar(usuarioId);
    if (usuario.getEstadoCuenta() != EstadoUsuario.ACTIVO) {
      throw new IllegalStateException("La cuenta debe estar activa.");
    }
    if (socios.findByUsuarioId(usuarioId).isPresent()) {
      throw new IllegalStateException("La persona ya está registrada como socio.");
    }
    NivelMembresia n = nivelId == null ? null : nivel(nivelId, relacion);
    Socio s = new Socio(usuario, relacion, EstadoVerificacionUnse.PENDIENTE, identificador);
    socios.save(s);
    Membresia m = membresias.save(new Membresia(s, n));
    s.setMembresia(m);
    auditar(s, actor, "Alta de socio", "Contratación pendiente de pago: " + n.getNombre());
    return detalle(s);
  }

  @Transactional(readOnly = true)
  public SocioDetalle consultar(UUID id, UUID actor) {
    Socio s = buscar(id);
    autorizar(s, actor);
    return detalle(s);
  }

  @Transactional(readOnly = true)
  public SocioDetalle actual(UUID actor) {
    return socios.findByUsuarioId(actor).map(this::detalle).orElse(null);
  }

  @Transactional(readOnly = true)
  public List<SocioDetalle> listar(String criterio, EstadoMembresia estado, RelacionUnse relacion) {
    String q = criterio == null ? "" : criterio.trim().toLowerCase(java.util.Locale.ROOT);
    return socios.buscar(q, estado, relacion).stream().map(this::detalle).toList();
  }

  public SocioDetalle actualizar(
      UUID id,
      RelacionUnse relacion,
      EstadoVerificacionUnse verificacion,
      String identificador,
      UUID nivelId,
      EstadoMembresia estado,
      String motivo,
      UUID actor) {
    Socio s = buscar(id);
    String antes = resumen(s);
    if (usuarios.buscar(actor).getRol() != RolUsuario.ADMIN) {
      throw new OperacionNoPermitidaException();
    }
    s.actualizarDatos(relacion, verificacion, identificador);
    if (s.getMembresia() != null) {
      if (nivelId == null || estado == null) {
        throw new IllegalArgumentException("Indicá nivel y estado de la membresía existente.");
      }
      if (!s.getMembresia().getNivelMembresia().getId().equals(nivelId)) {
        s.getMembresia().setNivelMembresia(nivel(nivelId, relacion));
      }
      s.getMembresia().cambiarEstado(estado);
    } else if (nivelId != null || estado != null) {
      throw new IllegalArgumentException("Contratá una membresía antes de modificar su estado.");
    }
    auditar(s, actor, motivo, antes + " → " + resumen(s));
    return detalle(s);
  }

  public MembresiaDetalle contratar(UUID socioId, UUID nivelId, UUID actor) {
    Socio s = buscar(socioId);
    autorizar(s, actor);
    NivelMembresia n = nivel(nivelId, s.getRelacionUnse());
    if (s.getMembresia() == null) {
      s.setMembresia(membresias.save(new Membresia(s, n)));
    } else {
      s.getMembresia().renovarSolicitud(n);
    }
    auditar(s, actor, "Nueva contratación", resumen(s));
    return membresiaDetalle(s.getMembresia());
  }

  @Transactional(readOnly = true)
  public MembresiaDetalle obtenerMembresia(UUID id, UUID actor) {
    Membresia m = membresias.findById(id).orElseThrow(MembresiaNoEncontradaException::new);
    autorizar(m.getSocio(), actor);
    return membresiaDetalle(m);
  }

  public MembresiaDetalle cancelar(UUID id, String motivo, UUID actor) {
    Membresia m = membresias.findById(id).orElseThrow(MembresiaNoEncontradaException::new);
    autorizar(m.getSocio(), actor);
    m.cancelar();
    auditar(m.getSocio(), actor, motivo, "Cancelación de membresía");
    return membresiaDetalle(m);
  }

  @Transactional(readOnly = true)
  public List<CambioSocioDetalle> historial(UUID id) {
    buscar(id);
    return cambios.findAllBySocioIdOrderByFechaDesc(id).stream()
        .map(
            c ->
                new CambioSocioDetalle(
                    c.getId(), c.getResponsableId(), c.getMotivo(), c.getDetalle(), c.getFecha()))
        .toList();
  }

  private String resumen(Socio s) {
    if (s.getMembresia() == null) {
      return s.getRelacionUnse() + " / " + s.getEstadoVerificacionUnse() + " / Sin membresía";
    }
    return s.getRelacionUnse()
        + " / "
        + s.getEstadoVerificacionUnse()
        + " / "
        + s.getMembresia().getNivelMembresia().getNombre()
        + " / "
        + s.getMembresia().getEstado();
  }

  private void auditar(Socio s, UUID actor, String motivo, String detalle) {
    cambios.save(new CambioSocio(s.getId(), actor, motivo, detalle));
  }

  private MembresiaDetalle membresiaDetalle(Membresia m) {
    return new MembresiaDetalle(
        m.getId(),
        m.getSocio().getId(),
        m.getNivelMembresia().getId(),
        m.getNivelMembresia().getNombre(),
        m.getEstado(),
        m.getFechaAlta(),
        m.getFechaBaja(),
        m.getProximoVencimiento());
  }

  private SocioDetalle detalle(Socio s) {
    var u = s.getUsuario();
    var m = s.getMembresia();
    var n = m == null ? null : m.getNivelMembresia();
    return new SocioDetalle(
        s.getId(),
        u.getId(),
        u.getNombreCompleto(),
        u.getEmail(),
        u.getDni(),
        s.getRelacionUnse(),
        s.getEstadoVerificacionUnse(),
        s.getIdentificadorUnse(),
        m == null ? null : m.getId(),
        n == null ? null : n.getId(),
        n == null ? null : n.getNombre(),
        m == null ? null : m.getEstado(),
        m == null ? null : m.getProximoVencimiento());
  }
}
