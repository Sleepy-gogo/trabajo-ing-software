package edu.unse.sera.encuesta.control;

import edu.unse.sera.encuesta.entity.Encuesta;
import edu.unse.sera.encuesta.entity.EnvioEncuesta;
import edu.unse.sera.encuesta.entity.Pregunta;
import edu.unse.sera.encuesta.entity.TipoPregunta;
import edu.unse.sera.encuesta.persistence.EncuestaRepository;
import edu.unse.sera.encuesta.persistence.EnvioEncuestaRepository;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.reserva.entity.EstadoReserva;
import edu.unse.sera.reserva.entity.Reserva;
import edu.unse.sera.reserva.persistence.ReservaRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EncuestaService {
  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private final EncuestaRepository encuestas;
  private final EnvioEncuestaRepository envios;
  private final ReservaRepository reservas;
  private final EspacioRepository espacios;

  public EncuestaService(
      EncuestaRepository encuestas,
      EnvioEncuestaRepository envios,
      ReservaRepository reservas,
      EspacioRepository espacios) {
    this.encuestas = encuestas;
    this.envios = envios;
    this.reservas = reservas;
    this.espacios = espacios;
  }

  @Transactional
  public EncuestaDetalle crear(EncuestaDatos datos) {
    if (datos.espacioId() != null && !espacios.existsById(datos.espacioId())) {
      throw new edu.unse.sera.espacio.control.EspacioNoEncontradoException(datos.espacioId());
    }
    if (datos.preguntas() == null || datos.preguntas().stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Indicá las preguntas de la encuesta.");
    }
    var preguntas = new ArrayList<Pregunta>();
    for (var p : datos.preguntas()) {
      preguntas.add(
          new Pregunta(p.texto(), p.tipo(), p.obligatoria(), p.opciones(), preguntas.size()));
    }
    return detalle(
        encuestas.save(
            new Encuesta(
                datos.titulo(),
                datos.descripcion(),
                datos.espacioId(),
                datos.desde(),
                datos.hasta(),
                preguntas)));
  }

  public List<EncuestaDetalle> listar() {
    return encuestas.findAllByOrderByCreadaEnDesc().stream().map(this::detalle).toList();
  }

  @Transactional
  public EncuestaDetalle cambiarActiva(UUID id, boolean activa) {
    var encuesta = encuestas.bloquear(id).orElseThrow(() -> noEncontrada());
    encuesta.cambiarActiva(activa);
    return detalle(encuesta);
  }

  public List<EncuestaDetalle.Asignacion> disponibles(UUID actor) {
    var definiciones = encuestas.findAllByOrderByCreadaEnDesc();
    var resultado = new ArrayList<EncuestaDetalle.Asignacion>();
    for (var reserva : reservas.utilizadas(actor)) {
      for (var encuesta : definiciones) {
        if (corresponde(encuesta, reserva)) {
          resultado.add(asignacion(encuesta, reserva));
        }
      }
    }
    return resultado;
  }

  public EncuestaDetalle.Asignacion consultar(UUID id, UUID reservaId, UUID actor) {
    var encuesta = buscar(id);
    var reserva = reserva(reservaId, actor);
    validarAsociacion(encuesta, reserva);
    return asignacion(encuesta, reserva);
  }

  @Transactional
  public EncuestaDetalle.Asignacion responder(
      UUID id, UUID reservaId, UUID actor, Map<UUID, String> respuestas) {
    var encuesta = encuestas.bloquear(id).orElseThrow(() -> noEncontrada());
    var reserva = reserva(reservaId, actor);
    validarAsociacion(encuesta, reserva);
    if (!encuesta.disponible(LocalDate.now(ZONA))) {
      throw new IllegalStateException("La encuesta está cerrada o fuera de su período.");
    }
    if (envios.findByEncuestaIdAndReservaId(id, reservaId).isPresent()) {
      throw new IllegalStateException("Ya respondiste esta encuesta para la reserva.");
    }
    if (respuestas == null
        || respuestas.keySet().stream()
            .anyMatch(k -> encuesta.getPreguntas().stream().noneMatch(p -> p.getId().equals(k)))) {
      throw new IllegalArgumentException(
          "Las respuestas deben corresponder a las preguntas de esta encuesta.");
    }
    var validadas = new LinkedHashMap<UUID, String>();
    for (var pregunta : encuesta.getPreguntas()) {
      String valor = pregunta.validarRespuesta(respuestas.get(pregunta.getId()));
      if (!valor.isEmpty()) {
        validadas.put(pregunta.getId(), valor);
      }
    }
    envios.saveAndFlush(new EnvioEncuesta(id, reservaId, validadas));
    return asignacion(encuesta, reserva);
  }

  public EncuestaDetalle.Resultados resultados(UUID id) {
    var encuesta = buscar(id);
    var respuestas = envios.findAllByEncuestaIdOrderByEnviadaEnDesc(id);
    var estadisticas =
        encuesta.getPreguntas().stream()
            .map(
                p -> {
                  List<String> valores =
                      respuestas.stream()
                          .map(e -> e.getRespuestas().get(p.getId()))
                          .filter(Objects::nonNull)
                          .toList();
                  var distribucion = new LinkedHashMap<String, Long>();
                  if (p.getTipo() != TipoPregunta.TEXTO) {
                    valores.forEach(v -> distribucion.merge(v, 1L, Long::sum));
                  }
                  Double promedio =
                      p.getTipo() == TipoPregunta.CALIFICACION && !valores.isEmpty()
                          ? valores.stream().mapToInt(Integer::parseInt).average().orElseThrow()
                          : null;
                  return new EncuestaDetalle.Estadistica(
                      p.getId(), p.getTexto(), valores.size(), promedio, distribucion);
                })
            .toList();
    var detalleEnvios =
        respuestas.stream()
            .map(
                e -> {
                  var r = reservas.findById(e.getReservaId()).orElseThrow();
                  return new EncuestaDetalle.Resultado(
                      e.getId(),
                      r.getId(),
                      r.getUsuario().getNombreCompleto(),
                      r.getEspacio().getNombre(),
                      r.getFecha(),
                      e.getEnviadaEn(),
                      e.getRespuestas());
                })
            .toList();
    return new EncuestaDetalle.Resultados(
        detalle(encuesta), respuestas.size(), estadisticas, detalleEnvios);
  }

  private EncuestaDetalle.Asignacion asignacion(Encuesta encuesta, Reserva reserva) {
    var envio = envios.findByEncuestaIdAndReservaId(encuesta.getId(), reserva.getId());
    String estado =
        envio.isPresent()
            ? "RESPONDIDA"
            : encuesta.disponible(LocalDate.now(ZONA)) ? "DISPONIBLE" : "CERRADA";
    return new EncuestaDetalle.Asignacion(
        detalle(encuesta),
        reserva.getId(),
        reserva.getEspacio().getNombre(),
        reserva.getFecha(),
        estado,
        envio.map(EnvioEncuesta::getRespuestas).orElse(Map.of()),
        envio.map(EnvioEncuesta::getEnviadaEn).orElse(null));
  }

  private boolean corresponde(Encuesta e, Reserva r) {
    return e.getEspacioId() == null || e.getEspacioId().equals(r.getEspacio().getId());
  }

  private void validarAsociacion(Encuesta e, Reserva r) {
    if (!corresponde(e, r)
        || r.getEstado() != EstadoReserva.CONFIRMADA
        || r.getConsumidaEn() == null) {
      throw new IllegalStateException(
          "La encuesta requiere una reserva utilizada del espacio correspondiente.");
    }
  }

  private Reserva reserva(UUID id, UUID actor) {
    var reserva =
        reservas
            .findById(id)
            .orElseThrow(() -> new edu.unse.sera.reserva.control.ReservaNoEncontradaException());
    if (!reserva.getUsuario().getId().equals(actor)) {
      throw new OperacionNoPermitidaException();
    }
    return reserva;
  }

  private Encuesta buscar(UUID id) {
    return encuestas.findById(id).orElseThrow(() -> noEncontrada());
  }

  private RecursoNoEncontradoException noEncontrada() {
    return new EncuestaNoEncontradaException();
  }

  private EncuestaDetalle detalle(Encuesta e) {
    return new EncuestaDetalle(
        e.getId(),
        e.getTitulo(),
        e.getDescripcion(),
        e.getEspacioId(),
        e.getDesde(),
        e.getHasta(),
        e.isActiva(),
        e.getCreadaEn(),
        e.getPreguntas().stream()
            .map(
                p ->
                    new EncuestaDetalle.PreguntaDetalle(
                        p.getId(), p.getTexto(), p.getTipo(), p.isObligatoria(), p.getOpciones()))
            .toList());
  }
}
