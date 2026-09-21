package edu.unse.sera.shared.boundary;

import edu.unse.sera.shared.boundary.dto.ApiError;
import edu.unse.sera.shared.exception.RecursoNoEncontradoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(edu.unse.sera.shared.exception.OperacionNoPermitidaException.class)
  public ResponseEntity<ApiError> prohibido(RuntimeException exception) {
    return ResponseEntity.status(403)
        .body(new ApiError("acceso_denegado", exception.getMessage(), Map.of()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> datosInvalidos(IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
        .body(new ApiError("datos_invalidos", exception.getMessage(), Map.of()));
  }

  @ExceptionHandler({
    IllegalStateException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  public ResponseEntity<ApiError> conflicto(RuntimeException exception) {
    return ResponseEntity.status(409)
        .body(new ApiError("conflicto", exception.getMessage(), Map.of()));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> integridad(RuntimeException exception) {
    return ResponseEntity.status(409)
        .body(
            new ApiError(
                "conflicto",
                "Los datos se duplican o están relacionados con otro registro.",
                Map.of()));
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> formato(RuntimeException exception) {
    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                "datos_invalidos", "Revisá los tipos, fechas y valores enviados.", Map.of()));
  }

  @ExceptionHandler(RecursoNoEncontradoException.class)
  public ResponseEntity<ApiError> handleRecursoNoEncontrado(
      RecursoNoEncontradoException exception) {
    ApiError error = new ApiError("recurso_no_encontrado", exception.getMessage(), Map.of());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException exception) {
    Map<String, String> campos = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
    ApiError error = new ApiError("datos_invalidos", "Hay datos inválidos.", campos);
    return ResponseEntity.badRequest().body(error);
  }
}
