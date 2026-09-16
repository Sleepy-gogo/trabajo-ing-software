package edu.unse.sera.membresia.control;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record NivelMembresiaDetalle(
    UUID id,
    String nombre,
    String descripcion,
    BigDecimal importeMensualBase,
    String moneda,
    List<String> beneficios,
    List<String> serviciosIncluidos,
    List<String> condiciones,
    boolean disponibleParaContratar,
    List<TarifaMembresiaDetalle> tarifas) {}
