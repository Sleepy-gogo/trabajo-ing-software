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
  List<Map<String, Object>> uso(LocalDate desde, LocalDate hasta, String relacion, UUID espacio);
}
