package edu.unse.sera.espacio.boundary;

import edu.unse.sera.disponibilidad.control.DisponibilidadSuperpuestaException;
import edu.unse.sera.espacio.control.EspacioDuplicadoException;
import edu.unse.sera.shared.boundary.dto.ApiError;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EspacioExceptionHandler {

  @ExceptionHandler(EspacioDuplicadoException.class)
  public ResponseEntity<ApiError> handleEspacioDuplicado(EspacioDuplicadoException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("espacio_duplicado", exception.getMessage(), Map.of()));
  }

  @ExceptionHandler(DisponibilidadSuperpuestaException.class)
  public ResponseEntity<ApiError> handleDisponibilidadSuperpuesta(
      DisponibilidadSuperpuestaException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("disponibilidad_superpuesta", exception.getMessage(), Map.of()));
  }
}
