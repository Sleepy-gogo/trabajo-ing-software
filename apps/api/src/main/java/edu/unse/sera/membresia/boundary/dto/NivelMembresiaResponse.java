package edu.unse.sera.membresia.boundary.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record NivelMembresiaResponse(
    UUID id,
    String nombre,
    String descripcion,
    BigDecimal importeMensualBase,
    String moneda,
    List<String> beneficios,
    List<String> serviciosIncluidos,
    List<String> condiciones,
    boolean disponibleParaContratar,
    List<TarifaMembresiaResponse> tarifas) {}
