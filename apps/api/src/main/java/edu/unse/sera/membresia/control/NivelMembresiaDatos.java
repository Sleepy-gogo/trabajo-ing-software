package edu.unse.sera.membresia.control;

import java.math.BigDecimal;
import java.util.List;

/** Datos que Control recibe para crear o modificar un nivel. */
public record NivelMembresiaDatos(
    String nombre,
    String descripcion,
    BigDecimal importeMensualBase,
    List<String> beneficios,
    List<String> serviciosIncluidos,
    List<String> condiciones,
    boolean disponibleParaContratar,
    List<TarifaMembresiaDatos> tarifas) {}
