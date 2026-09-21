package edu.unse.sera.disponibilidad.control;

import edu.unse.sera.disponibilidad.entity.Bloqueo;
import edu.unse.sera.disponibilidad.entity.DiaSemana;
import edu.unse.sera.disponibilidad.persistence.BloqueoRepository;
import edu.unse.sera.disponibilidad.persistence.DisponibilidadRepository;
import edu.unse.sera.espacio.control.EspacioNoEncontradoException;
import edu.unse.sera.espacio.entity.EstadoEspacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.socio.entity.EstadoVerificacionUnse;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.socio.persistence.SocioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CalendarioService {
  private final EspacioRepository espacios;
  private final DisponibilidadRepository horarios;
  private final BloqueoRepository bloqueos;
  private final SocioRepository socios;

  public CalendarioService(
      EspacioRepository espacios,
      DisponibilidadRepository horarios,
      BloqueoRepository bloqueos,
      SocioRepository socios) {
    this.espacios = espacios;
    this.horarios = horarios;
    this.bloqueos = bloqueos;
    this.socios = socios;
  }

  public record Franja(LocalTime desde, LocalTime hasta) {}

  public record Calendario(
      LocalDate fecha,
      EstadoEspacio estado,
      BigDecimal tarifaHora,
      RelacionUnse relacionAplicada,
      List<Franja> franjas) {}

  public record BloqueoDetalle(
      UUID id, LocalDate fecha, LocalTime desde, LocalTime hasta, String motivo) {}

  @Transactional(readOnly = true)
  public Calendario consultar(UUID id, LocalDate fecha, UUID usuarioId) {
    var espacio = espacios.findById(id).orElseThrow(() -> new EspacioNoEncontradoException(id));
    RelacionUnse relacion =
        socios
            .findByUsuarioId(usuarioId)
            .filter(s -> s.getEstadoVerificacionUnse() == EstadoVerificacionUnse.VERIFICADA)
            .map(s -> s.getRelacionUnse())
            .orElse(RelacionUnse.EXTERNO);
    List<Franja> libres = new ArrayList<>();
    if (espacio.getEstado() == EstadoEspacio.HABILITADO && !fecha.isBefore(LocalDate.now())) {
      DiaSemana dia = DiaSemana.values()[fecha.getDayOfWeek().getValue() - 1];
      var cortes = bloqueos.findAllByEspacioIdAndFechaOrderByDesde(id, fecha);
      for (var h : horarios.findAllByEspacioId(id)) {
        if (h.getDiaSemana() != dia) {
          continue;
        }
        LocalTime cursor = h.getHoraDesde();
        for (var b : cortes) {
          if (!b.getHasta().isAfter(cursor) || !b.getDesde().isBefore(h.getHoraHasta())) {
            continue;
          }
          if (b.getDesde().isAfter(cursor)) {
            libres.add(new Franja(cursor, b.getDesde()));
          }
          if (b.getHasta().isAfter(cursor)) {
            cursor = b.getHasta();
          }
          if (!cursor.isBefore(h.getHoraHasta())) {
            break;
          }
        }
        if (cursor.isBefore(h.getHoraHasta())) {
          libres.add(new Franja(cursor, h.getHoraHasta()));
        }
      }
    }
    var ahora = LocalTime.now();
    libres =
        libres.stream()
            .filter(f -> !fecha.equals(LocalDate.now()) || f.hasta().isAfter(ahora))
            .map(
                f ->
                    fecha.equals(LocalDate.now()) && f.desde().isBefore(ahora)
                        ? new Franja(ahora.withNano(0), f.hasta())
                        : f)
            .sorted(java.util.Comparator.comparing(Franja::desde))
            .toList();
    return new Calendario(
        fecha, espacio.getEstado(), espacio.tarifaPara(relacion), relacion, libres);
  }

  public BloqueoDetalle bloquear(
      UUID id, LocalDate fecha, LocalTime desde, LocalTime hasta, String motivo) {
    espacios.bloquearPorId(id).orElseThrow(() -> new EspacioNoEncontradoException(id));
    Bloqueo nuevo = new Bloqueo(id, fecha, desde, hasta, motivo);
    if (bloqueos.findAllByEspacioIdAndFechaOrderByDesde(id, fecha).stream()
        .anyMatch(b -> b.getDesde().isBefore(hasta) && b.getHasta().isAfter(desde))) {
      throw new IllegalStateException("El bloqueo se superpone con otro existente.");
    }
    return detalle(bloqueos.save(nuevo));
  }

  @Transactional(readOnly = true)
  public List<BloqueoDetalle> listar(UUID id, LocalDate fecha) {
    if (!espacios.existsById(id)) {
      throw new EspacioNoEncontradoException(id);
    }
    return bloqueos.findAllByEspacioIdAndFechaOrderByDesde(id, fecha).stream()
        .map(this::detalle)
        .toList();
  }

  public void eliminar(UUID id, UUID bloqueoId) {
    espacios.bloquearPorId(id).orElseThrow(() -> new EspacioNoEncontradoException(id));
    var b =
        bloqueos
            .findById(bloqueoId)
            .filter(v -> v.getEspacioId().equals(id))
            .orElseThrow(() -> new DisponibilidadNoEncontradaException(bloqueoId));
    bloqueos.delete(b);
  }

  private BloqueoDetalle detalle(Bloqueo b) {
    return new BloqueoDetalle(b.getId(), b.getFecha(), b.getDesde(), b.getHasta(), b.getMotivo());
  }
}
