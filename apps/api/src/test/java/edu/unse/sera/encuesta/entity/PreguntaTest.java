package edu.unse.sera.encuesta.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PreguntaTest {
  @Test
  void validaObligatoriedadEscalaYTexto() {
    var p = new Pregunta("Experiencia", TipoPregunta.CALIFICACION, true, List.of(), 0);
    assertThat(p.validarRespuesta(" 5 ")).isEqualTo("5");
    for (String valor : new String[] {"", "0", "6", "muy buena", "1.5"}) {
      assertThatThrownBy(() -> p.validarRespuesta(valor))
          .isInstanceOf(IllegalArgumentException.class);
    }
    var texto = new Pregunta("Comentario", TipoPregunta.TEXTO, false, List.of(), 1);
    assertThat(texto.validarRespuesta(null)).isEmpty();
    assertThatThrownBy(() -> texto.validarRespuesta("a".repeat(2001)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void opcionesDebenSerDistintasYLaRespuestaDebePertenecer() {
    assertThatThrownBy(
            () -> new Pregunta("Volverías", TipoPregunta.OPCION, true, List.of("Sí", " Sí "), 0))
        .isInstanceOf(IllegalArgumentException.class);
    var opcion = new Pregunta("Volverías", TipoPregunta.OPCION, true, List.of("Sí", "No"), 0);
    assertThat(opcion.validarRespuesta("No")).isEqualTo("No");
    assertThatThrownBy(() -> opcion.validarRespuesta("Quizás"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new Pregunta(
                    "Calificación", TipoPregunta.CALIFICACION, true, List.of("Sí", "No"), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
