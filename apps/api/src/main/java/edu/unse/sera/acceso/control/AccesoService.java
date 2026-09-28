package edu.unse.sera.acceso.control;

import edu.unse.sera.espacio.entity.EstadoEspacio;
import edu.unse.sera.membresia.persistence.MembresiaRepository;
import edu.unse.sera.reserva.entity.EstadoReserva;
import edu.unse.sera.reserva.entity.Reserva;
import edu.unse.sera.reserva.persistence.ReservaRepository;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccesoService {
  private final ReservaRepository reservas;
  private final UsuarioRepository usuarios;
  private final MembresiaRepository membresias;

  public AccesoService(
      ReservaRepository reservas, UsuarioRepository usuarios, MembresiaRepository membresias) {
    this.reservas = reservas;
    this.usuarios = usuarios;
    this.membresias = membresias;
  }

  public AccesoResultado validar(String codigo) {
    return validar(codigo, OffsetDateTime.now(Reserva.ZONA));
  }

  AccesoResultado validar(String codigo, OffsetDateTime ahora) {
    String valor = codigo == null ? "" : codigo.trim();
    if (valor.matches("SERA-U[a-z0-9]{12}")) {
      return usuarios
          .findByQrUsuario(valor)
          .map(
              u -> {
                boolean activo = u.getEstadoCuenta() == EstadoUsuario.ACTIVO;
                boolean vigente =
                    activo
                        && membresias
                            .buscarMembresiaPorUsuarioId(u.getId())
                            .map(
                                m ->
                                    m.estaVigente(
                                        ahora.atZoneSameInstant(Reserva.ZONA).toLocalDate()))
                            .orElse(false);
                String motivo =
                    !activo
                        ? "La cuenta no está activa."
                        : vigente
                            ? "Membresía vigente. Acceso general habilitado; no incluye una reserva de espacio."
                            : "Sin membresía vigente. Presentá el QR de una reserva confirmada.";
                return new AccesoResultado(
                    vigente, motivo, "PERSONAL", u.getNombreCompleto(), null, null, null, null);
              })
          .orElseGet(this::invalido);
    }
    if (!valor.matches(
        "SERA-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
      return invalido();
    }
    return reservas
        .findByCodigo(valor)
        .map(r -> validarReserva(r, ahora))
        .orElseGet(this::invalido);
  }

  private AccesoResultado validarReserva(Reserva r, OffsetDateTime ahora) {
    String motivo = null;
    if (r.getUsuario().getEstadoCuenta() != EstadoUsuario.ACTIVO) {
      motivo = "La cuenta no está activa.";
    } else if (r.getEstado() != EstadoReserva.CONFIRMADA) {
      motivo = "La reserva no está confirmada o fue cancelada.";
    } else if (r.getEspacio().getEstado() != EstadoEspacio.HABILITADO) {
      motivo = "El espacio no está habilitado.";
    } else {
      var instante = ahora.toInstant();
      var inicio = r.getFecha().atTime(r.getDesde()).atZone(Reserva.ZONA).toInstant();
      var fin = r.getFecha().atTime(r.getHasta()).atZone(Reserva.ZONA).toInstant();
      if (instante.isBefore(inicio)) {
        motivo = "La reserva todavía no comenzó.";
      } else if (!instante.isBefore(fin)) {
        motivo = "El horario de la reserva ya finalizó.";
      }
    }
    return new AccesoResultado(
        motivo == null,
        motivo == null ? "Reserva confirmada y dentro del horario reservado." : motivo,
        "RESERVA",
        r.getUsuario().getNombreCompleto(),
        r.getEspacio().getNombre(),
        r.getFecha(),
        r.getDesde(),
        r.getHasta());
  }

  private AccesoResultado invalido() {
    return new AccesoResultado(
        false,
        "Código no reconocido. Usá un QR o código emitido por SERA.",
        "DESCONOCIDO",
        null,
        null,
        null,
        null,
        null);
  }
}
