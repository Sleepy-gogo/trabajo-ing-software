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
