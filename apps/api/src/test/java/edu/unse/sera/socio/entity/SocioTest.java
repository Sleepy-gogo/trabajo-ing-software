package edu.unse.sera.socio.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SocioTest {
  @ParameterizedTest
  @EnumSource(RelacionUnse.class)
  void soloElExternoQuedaVerificadoAlRegistrarse(RelacionUnse relacion) {
    assertThat(Socio.verificacionInicial(relacion))
        .isEqualTo(
            relacion == RelacionUnse.EXTERNO
                ? EstadoVerificacionUnse.VERIFICADA
                : EstadoVerificacionUnse.PENDIENTE);
  }

  @ParameterizedTest
  @EnumSource(EstadoVerificacionUnse.class)
  void soloUnaRelacionVerificadaHabilitaElIngreso(EstadoVerificacionUnse estado) {
    Socio socio = new Socio(null, RelacionUnse.ESTUDIANTE, estado, null);
    assertThat(socio.habilitaIngreso()).isEqualTo(estado == EstadoVerificacionUnse.VERIFICADA);
  }
}
