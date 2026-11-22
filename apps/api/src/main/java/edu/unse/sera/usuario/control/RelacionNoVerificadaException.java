package edu.unse.sera.usuario.control;

import edu.unse.sera.socio.entity.EstadoVerificacionUnse;

public class RelacionNoVerificadaException extends RuntimeException {
  private final EstadoVerificacionUnse estado;

  public RelacionNoVerificadaException(EstadoVerificacionUnse estado) {
    super(
        estado == EstadoVerificacionUnse.RECHAZADA
            ? "Administración no pudo verificar tu relación con la UNSE. Acercate a"
                + " administración del polideportivo para revisarla."
            : "Tu relación con la UNSE todavía no fue verificada. Vas a poder ingresar cuando"
                + " administración la confirme.");
    this.estado = estado;
  }

  public EstadoVerificacionUnse getEstado() {
    return estado;
  }
}
