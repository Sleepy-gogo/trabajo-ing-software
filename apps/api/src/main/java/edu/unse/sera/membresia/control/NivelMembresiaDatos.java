package edu.unse.sera.membresia.control;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Datos que Control recibe para crear o modificar un nivel. */
public record NivelMembresiaDatos(
    String nombre,
    String descripcion,
    Map<RelacionUnse, BigDecimal> preciosPorRelacion,
    List<String> beneficios,
    List<String> serviciosIncluidos,
    List<String> condiciones,
    boolean disponibleParaContratar) {}
