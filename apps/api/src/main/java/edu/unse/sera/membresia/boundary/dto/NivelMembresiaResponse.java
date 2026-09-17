package edu.unse.sera.membresia.boundary.dto;

import edu.unse.sera.socio.entity.RelacionUnse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NivelMembresiaResponse(
    UUID id,
    String nombre,
    String descripcion,
    Map<RelacionUnse, BigDecimal> preciosPorRelacion,
    String moneda,
    List<String> beneficios,
    List<String> serviciosIncluidos,
    List<String> condiciones,
    boolean disponibleParaContratar) {}
