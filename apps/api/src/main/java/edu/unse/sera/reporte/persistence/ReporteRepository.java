package edu.unse.sera.reporte.persistence;

import edu.unse.sera.reserva.entity.Reserva;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface ReporteRepository extends Repository<Reserva, UUID> {
  @Query(
      value =
          """
      WITH socios_actuales AS (
        SELECT s.id, u.nombre_completo AS nombre, u.email, s.relacion_unse AS relacion,
          CASE WHEN u.estado_cuenta <> 'ACTIVO' THEN u.estado_cuenta
            WHEN m.id IS NULL THEN 'SIN_MEMBRESIA'
            WHEN m.estado IN ('ACTIVA','VENCIDA') THEN
              CASE WHEN m.proximo_vencimiento > :hoy THEN 'ACTIVA' ELSE 'VENCIDA' END
            ELSE m.estado END AS estado,
          COALESCE(n.nombre, '') AS nivel,
          (s.created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date AS alta
        FROM socios s JOIN usuarios u ON u.id = s.usuario_id
        LEFT JOIN membresias m ON m.socio_id = s.id
        LEFT JOIN nivel_membresias n ON n.id = m.nivel_membresia_id
      ) SELECT * FROM socios_actuales
      WHERE alta BETWEEN :desde AND :hasta
        AND (:estado = '' OR estado = :estado)
        AND (:relacion = '' OR relacion = :relacion)
      ORDER BY nombre, id LIMIT 5001
      """,
      nativeQuery = true)
  List<Map<String, Object>> socios(
      LocalDate desde, LocalDate hasta, String estado, String relacion, LocalDate hoy);

  @Query(
      value =
          """
      WITH reservas_actuales AS (
        SELECT r.id, u.nombre_completo AS titular, e.nombre AS espacio, r.espacio_id,
          r.fecha, r.desde, r.hasta, r.total, r.personas, r.relacion_aplicada AS relacion,
          CASE WHEN r.estado <> 'CONFIRMADA' THEN r.estado
            WHEN r.consumida_en IS NOT NULL THEN 'CONSUMIDA'
            WHEN r.fecha + r.hasta <= :ahora THEN 'FINALIZADA'
            WHEN r.fecha + r.desde <= :ahora THEN 'EN_CURSO' ELSE 'CONFIRMADA' END AS estado
        FROM reservas r JOIN usuarios u ON u.id = r.usuario_id JOIN espacios e ON e.id = r.espacio_id
      ) SELECT * FROM reservas_actuales
      WHERE fecha BETWEEN :desde AND :hasta
        AND (:estado = '' OR estado = :estado)
        AND (:relacion = '' OR relacion = :relacion)
        AND (CAST(:espacio AS uuid) IS NULL OR espacio_id = CAST(:espacio AS uuid))
      ORDER BY fecha, desde, id LIMIT 5001
      """,
      nativeQuery = true)
  List<Map<String, Object>> reservas(
      LocalDate desde,
      LocalDate hasta,
      String estado,
      String relacion,
      UUID espacio,
      LocalDateTime ahora);

  @Query(
      value =
          """
      SELECT p.id, u.nombre_completo AS titular, p.concepto, p.estado, p.medio_pago AS medio,
        p.monto, (p.created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date AS fecha,
        p.requiere_revision AS revision
      FROM pagos p JOIN usuarios u ON u.id = p.usuario_id
      LEFT JOIN reservas r ON r.id = p.reserva_id
      WHERE (p.created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date BETWEEN :desde AND :hasta
        AND (:estado = '' OR p.estado = :estado)
        AND (:relacion = '' OR p.relacion_aplicada = :relacion)
        AND (CAST(:espacio AS uuid) IS NULL OR r.espacio_id = CAST(:espacio AS uuid))
      ORDER BY p.created_at, p.id LIMIT 5001
      """,
      nativeQuery = true)
  List<Map<String, Object>> pagos(
      LocalDate desde, LocalDate hasta, String estado, String relacion, UUID espacio);

  @Query(
      value =
          """
      SELECT e.id, e.nombre AS espacio, COUNT(r.id) AS reservas,
        COUNT(r.id) FILTER (WHERE r.fecha + r.hasta <= :ahora) AS terminadas,
        COUNT(r.consumida_en) AS ingresos,
        COALESCE(SUM(EXTRACT(EPOCH FROM (r.hasta - r.desde)) / 3600), 0) AS horas,
        COALESCE(SUM(CASE WHEN r.consumida_en IS NOT NULL THEN r.personas ELSE 0 END), 0) AS personas
      FROM espacios e LEFT JOIN reservas r ON r.espacio_id = e.id
        AND r.fecha BETWEEN :desde AND :hasta AND r.estado = 'CONFIRMADA'
        AND (:relacion = '' OR r.relacion_aplicada = :relacion)
      WHERE (CAST(:espacio AS uuid) IS NULL OR e.id = CAST(:espacio AS uuid))
      GROUP BY e.id, e.nombre ORDER BY e.nombre, e.id
      """,
      nativeQuery = true)
  List<Map<String, Object>> uso(
      LocalDate desde, LocalDate hasta, String relacion, UUID espacio, LocalDateTime ahora);

  @Query(
      value =
          """
      SELECT a.id,
        to_char(a.creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires', 'YYYY-MM-DD HH24:MI:SS')
          AS fecha,
        a.tipo,
        CASE a.tipo
          WHEN 'INICIO_SESION' THEN 'Inicio de sesión'
          WHEN 'INICIO_SESION_FALLIDO' THEN 'Inicio de sesión fallido'
          WHEN 'INICIO_SESION_BLOQUEADO' THEN 'Inicio de sesión bloqueado'
          WHEN 'CIERRE_SESION' THEN 'Cierre de sesión'
          WHEN 'RESERVA_CREADA' THEN 'Reserva creada'
          WHEN 'RESERVA_CANCELADA' THEN 'Reserva cancelada'
          WHEN 'VERIFICACION_ACTUALIZADA' THEN 'Verificación actualizada'
          WHEN 'INFORME_GENERADO' THEN 'Informe generado'
          ELSE a.tipo END AS accion,
        COALESCE(a.email, '') AS usuario, COALESCE(u.nombre_completo, '') AS nombre,
        COALESCE(a.detalle, '') AS detalle
      FROM registros_actividad a LEFT JOIN usuarios u ON u.id = a.usuario_id
      WHERE (a.creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
          BETWEEN :desde AND :hasta
        AND (:tipo = '' OR a.tipo = :tipo)
      ORDER BY a.creado_en DESC, a.id LIMIT 5001
      """,
      nativeQuery = true)
  List<Map<String, Object>> actividad(LocalDate desde, LocalDate hasta, String tipo);

  // Consultas del panel de indicadores. Los ingresos usan pagos aprobados por fecha de
  // aprobación; reservas, horas y demanda usan reservas confirmadas por fecha de la reserva.

  @Query(
      value =
          """
      SELECT
        (SELECT COALESCE(SUM(p.monto), 0) FROM pagos p
          WHERE p.estado = 'APROBADO' AND p.aprobado_en IS NOT NULL
            AND (p.aprobado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
              BETWEEN :desde AND :hasta) AS ingresos,
        COUNT(r.id) FILTER (WHERE r.estado = 'CONFIRMADA') AS confirmadas,
        COUNT(r.id) FILTER (WHERE r.estado = 'CANCELADA') AS canceladas,
        COALESCE(SUM(EXTRACT(EPOCH FROM (r.hasta - r.desde)) / 3600)
          FILTER (WHERE r.estado = 'CONFIRMADA'), 0) AS horas,
        COUNT(r.id) FILTER (WHERE r.estado = 'CONFIRMADA' AND r.fecha + r.hasta <= :ahora)
          AS terminadas,
        COUNT(r.id) FILTER (WHERE r.estado = 'CONFIRMADA' AND r.fecha + r.hasta <= :ahora
          AND r.consumida_en IS NOT NULL) AS con_ingreso
      FROM reservas r
      WHERE r.fecha BETWEEN :desde AND :hasta
      """,
      nativeQuery = true)
  List<Map<String, Object>> resumenPeriodo(LocalDate desde, LocalDate hasta, LocalDateTime ahora);

  @Query(
      value =
          """
      SELECT to_char(p.aprobado_en AT TIME ZONE 'America/Argentina/Buenos_Aires', 'YYYY-MM') AS mes,
        COALESCE(SUM(p.monto) FILTER (WHERE p.concepto = 'CUOTA_MENSUAL'), 0) AS cuotas,
        COALESCE(SUM(p.monto) FILTER (WHERE p.concepto IN ('RESERVA', 'DIFERENCIA_TICKET')), 0)
          AS reservas
      FROM pagos p
      WHERE p.estado = 'APROBADO' AND p.aprobado_en IS NOT NULL
        AND (p.aprobado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
          BETWEEN :desde AND :hasta
      GROUP BY 1 ORDER BY 1
      """,
      nativeQuery = true)
  List<Map<String, Object>> ingresosPorMes(LocalDate desde, LocalDate hasta);

  @Query(
      value =
          """
      SELECT e.nombre AS espacio,
        COALESCE(h.reservas, 0) AS reservas, COALESCE(h.horas, 0) AS horas,
        COALESCE(i.ingresos, 0) AS ingresos
      FROM espacios e
      LEFT JOIN (
        SELECT r.espacio_id, COUNT(*) AS reservas,
          SUM(EXTRACT(EPOCH FROM (r.hasta - r.desde)) / 3600) AS horas
        FROM reservas r
        WHERE r.estado = 'CONFIRMADA' AND r.fecha BETWEEN :desde AND :hasta
        GROUP BY r.espacio_id) h ON h.espacio_id = e.id
      LEFT JOIN (
        SELECT r.espacio_id, SUM(p.monto) AS ingresos
        FROM pagos p JOIN reservas r ON r.id = p.reserva_id
        WHERE p.estado = 'APROBADO' AND p.aprobado_en IS NOT NULL
          AND (p.aprobado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
            BETWEEN :desde AND :hasta
        GROUP BY r.espacio_id) i ON i.espacio_id = e.id
      WHERE h.espacio_id IS NOT NULL OR i.espacio_id IS NOT NULL OR e.estado = 'HABILITADO'
      ORDER BY horas DESC, ingresos DESC, e.nombre
      """,
      nativeQuery = true)
  List<Map<String, Object>> usoPorEspacio(LocalDate desde, LocalDate hasta);

  /** Cada hora que ocupa una reserva suma una unidad: una reserva de 18 a 20 cuenta en 18 y 19. */
  @Query(
      value =
          """
      SELECT EXTRACT(ISODOW FROM r.fecha)::int AS dia, h.hora, COUNT(*) AS reservas
      FROM reservas r
      CROSS JOIN LATERAL generate_series(
        EXTRACT(HOUR FROM r.desde)::int,
        EXTRACT(HOUR FROM (r.hasta - INTERVAL '1 minute'))::int) AS h(hora)
      WHERE r.estado = 'CONFIRMADA' AND r.fecha BETWEEN :desde AND :hasta
      GROUP BY 1, 2 ORDER BY 1, 2
      """,
      nativeQuery = true)
  List<Map<String, Object>> demanda(LocalDate desde, LocalDate hasta);

  @Query(
      value =
          """
      SELECT s.relacion_unse AS relacion, COUNT(*) AS cantidad,
        COUNT(*) FILTER (WHERE s.estado_verificacion_unse = 'PENDIENTE') AS pendientes
      FROM socios s JOIN usuarios u ON u.id = s.usuario_id
      WHERE u.rol = 'USUARIO' AND u.estado_cuenta = 'ACTIVO'
      GROUP BY s.relacion_unse ORDER BY cantidad DESC, relacion
      """,
      nativeQuery = true)
  List<Map<String, Object>> sociosPorRelacion();

  /**
   * Membresías con una cuota aprobada que cubre la fecha indicada. Cada cuota aplicada cubre el mes
   * anterior a su vencimiento resultante.
   */
  @Query(
      value =
          """
      SELECT n.nombre AS nivel, COUNT(DISTINCT m.id) AS cantidad
      FROM membresias m JOIN nivel_membresias n ON n.id = m.nivel_membresia_id
      WHERE (m.fecha_baja IS NULL OR m.fecha_baja > :fecha)
        AND EXISTS (
          SELECT 1 FROM pagos p
          WHERE p.membresia_id = m.id AND p.estado = 'APROBADO'
            AND p.concepto = 'CUOTA_MENSUAL' AND p.vencimiento_resultante > :fecha
            AND (p.vencimiento_resultante - INTERVAL '1 month')::date <= :fecha)
      GROUP BY n.nombre ORDER BY cantidad DESC, nivel
      """,
      nativeQuery = true)
  List<Map<String, Object>> membresiasActivasPorNivel(LocalDate fecha);
}
