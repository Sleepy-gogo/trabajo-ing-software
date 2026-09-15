package edu.unse.sera.espacio.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EspacioTest {

  @Test
  void rechazaUnNombreVacio() {
    assertThatThrownBy(() -> new Espacio("  ", null, 0, "tipo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("El nombre es obligatorio.");
  }

  @Test
  void rechazaUnNombreDemasiadoLargo() {
    assertThatThrownBy(() -> new Espacio("a".repeat(101), null, 0, "tipo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("El nombre no puede superar los 100 caracteres.");
  }

  @Test
  void rechazaUnaDescripcionDemasiadoLargaSinModificarElEstado() {
    Espacio espacio = new Espacio("Cancha cubierta", "Piso de parquet", 20, "futsal");

    assertThatThrownBy(() -> espacio.actualizar("Cancha norte", "a".repeat(501), 20, "futsal"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("La descripción no puede superar los 500 caracteres.");
    assertThat(espacio.getNombre()).isEqualTo("Cancha cubierta");
    assertThat(espacio.getDescripcion()).isEqualTo("Piso de parquet");
  }
}
