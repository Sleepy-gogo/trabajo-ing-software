package edu.unse.sera.reporte.control;

import edu.unse.sera.espacio.control.EspacioNoEncontradoException;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import edu.unse.sera.reporte.entity.Informe;
import edu.unse.sera.reporte.persistence.InformeRepository;
import edu.unse.sera.reporte.persistence.ReporteRepository;
import edu.unse.sera.usuario.control.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ReporteService {
  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private final ReporteRepository datos;
  private final InformeRepository informes;
  private final UsuarioRepository usuarios;
  private final ObjectMapper mapper;
  private final EspacioRepository espacios;

  public ReporteService(
      ReporteRepository datos,
      InformeRepository informes,
      UsuarioRepository usuarios,
      ObjectMapper mapper,
      EspacioRepository espacios) {
    this.datos = datos;
    this.informes = informes;
    this.usuarios = usuarios;
    this.mapper = mapper;
    this.espacios = espacios;
  }

  @Transactional
  public ReporteDetalle generar(ReporteFiltros f, UUID actor) {
    var ahora = OffsetDateTime.now(ZONA);
    String espacioNombre =
        f.espacioId() == null
            ? null
            : espacios
                .findById(f.espacioId())
                .orElseThrow(() -> new EspacioNoEncontradoException(f.espacioId()))
                .getNombre();
    List<Map<String, Object>> filas =
        switch (f.tipo()) {
          case "socios" ->
              datos.socios(f.desde(), f.hasta(), f.estado(), f.relacion(), ahora.toLocalDate());
          case "reservas" ->
              datos.reservas(
                  f.desde(),
                  f.hasta(),
                  f.estado(),
                  f.relacion(),
                  f.espacioId(),
                  ahora.toLocalDateTime());
          case "pagos" ->
              datos.pagos(f.desde(), f.hasta(), f.estado(), f.relacion(), f.espacioId());
          default -> datos.uso(f.desde(), f.hasta(), f.relacion(), f.espacioId());
        };
    if (filas.size() > 5000) {
      throw new IllegalArgumentException(
          "El informe supera 5000 filas. Reducí el período o agregá filtros.");
    }
    // Se convierten tipos JDBC a valores JSON antes de guardar la instantánea.
    List<Map<String, Object>> normalizadas = filas.stream().map(this::normalizar).toList();
    Map<String, Object> resumen = new LinkedHashMap<>();
    resumen.put("registros", filas.size());
    if (f.tipo().equals("pagos")) {
      BigDecimal aprobados =
          filas.stream()
              .filter(r -> "APROBADO".equals(r.get("estado")))
              .map(r -> new BigDecimal(r.get("monto").toString()))
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      resumen.put("importe_aprobado", aprobados);
    }
    var usuario =
        usuarios.findById(actor).orElseThrow(() -> new UsuarioNoEncontradoException(actor));
    var detalle =
        new ReporteDetalle(
            UUID.randomUUID(),
            f.tipo(),
            f,
            ahora.withOffsetSameInstant(java.time.ZoneOffset.UTC),
            usuario.getNombreCompleto(),
            espacioNombre,
            columnas(f.tipo()),
            normalizadas,
            resumen);
    informes.save(
        new Informe(detalle.id(), f.tipo(), ahora, actor, mapper.writeValueAsString(detalle)));
    return detalle;
  }

  public List<ReporteDetalle> recientes(int pagina) {
    if (pagina < 0) {
      throw new IllegalArgumentException("La página no puede ser negativa.");
    }
    return informes
        .findAll(PageRequest.of(pagina, 20, Sort.by(Sort.Direction.DESC, "creadoEn")))
        .stream()
        .map(this::leer)
        .toList();
  }

  public ReporteDetalle consultar(UUID id) {
    return leer(informes.findById(id).orElseThrow(() -> new InformeNoEncontradoException()));
  }

  private ReporteDetalle leer(Informe informe) {
    return mapper.readValue(informe.getContenido(), ReporteDetalle.class);
  }

  private Map<String, Object> normalizar(Map<String, Object> fila) {
    var resultado = new LinkedHashMap<String, Object>();
    fila.forEach(
        (key, value) ->
            resultado.put(
                key,
                value == null || value instanceof Number || value instanceof Boolean
                    ? value
                    : value.toString()));
    return resultado;
  }

  private List<ReporteDetalle.Columna> columnas(String tipo) {
    String[][] campos =
        switch (tipo) {
          case "socios" ->
              new String[][] {
                {"nombre", "Socio"},
                {"email", "Email"},
                {"relacion", "Relación UNSE"},
                {"estado", "Estado actual"},
                {"nivel", "Nivel"},
                {"alta", "Alta"}
              };
          case "reservas" ->
              new String[][] {
                {"titular", "Titular"},
                {"espacio", "Espacio"},
                {"fecha", "Fecha"},
                {"desde", "Desde"},
                {"hasta", "Hasta"},
                {"estado", "Estado actual"},
                {"personas", "Personas"},
                {"total", "Total ARS"}
              };
          case "pagos" ->
              new String[][] {
                {"titular", "Titular"},
                {"concepto", "Concepto"},
                {"estado", "Estado"},
                {"medio", "Medio"},
                {"monto", "Monto ARS"},
                {"fecha", "Creación"},
                {"revision", "Requiere revisión"}
              };
          default ->
              new String[][] {
                {"espacio", "Espacio"},
                {"reservas", "Reservas confirmadas"},
                {"ingresos", "Ingresos registrados"},
                {"horas", "Horas reservadas"},
                {"personas", "Personas en reservas utilizadas"}
              };
        };
    return java.util.Arrays.stream(campos)
        .map(c -> new ReporteDetalle.Columna(c[0], c[1]))
        .toList();
  }

  public String csv(UUID id) {
    var informe = consultar(id);
    var csv = new StringBuilder("\uFEFF");
    csv.append(
            informe.columnas().stream()
                .map(c -> celdaCsv(c.label()))
                .collect(java.util.stream.Collectors.joining(",")))
        .append("\r\n");
    for (var fila : informe.filas()) {
      csv.append(
              informe.columnas().stream()
                  .map(c -> celdaInforme(c.key(), fila.get(c.key())))
                  .collect(java.util.stream.Collectors.joining(",")))
          .append("\r\n");
    }
    return csv.toString();
  }

  static String celdaCsv(Object valor) {
    String texto = valor == null ? "" : valor.toString();
    if (valor instanceof String
        && !texto.stripLeading().isEmpty()
        && "=+@-".indexOf(texto.stripLeading().charAt(0)) >= 0) {
      texto = "'" + texto;
    }
    return "\"" + texto.replace("\"", "\"\"") + "\"";
  }

  private String celdaInforme(String key, Object valor) {
    if (valor instanceof Number && (key.equals("monto") || key.equals("total"))) {
      return celdaCsv(new BigDecimal(valor.toString()).setScale(2));
    }
    return celdaCsv(valor);
  }
}
