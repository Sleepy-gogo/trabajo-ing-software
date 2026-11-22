package edu.unse.sera.membresia.control;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record NivelMembresiaDatos(
    String nombre,
    String descripcion,
    Map<RelacionUnse, BigDecimal> preciosPorRelacion,
    List<String> beneficios,
    boolean disponibleParaContratar) {}
