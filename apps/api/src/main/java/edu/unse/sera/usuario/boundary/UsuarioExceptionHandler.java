package edu.unse.sera.usuario.boundary;

import edu.unse.sera.shared.boundary.dto.ApiError;
import edu.unse.sera.usuario.control.UsuarioDuplicadoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UsuarioExceptionHandler {

  @ExceptionHandler(UsuarioDuplicadoException.class)
  public ResponseEntity<ApiError> handleUsuarioDuplicado(UsuarioDuplicadoException exception) {
    ApiError error =
        new ApiError(
            "usuario_duplicado",
            exception.getMessage(),
            Map.of(exception.getCampo(), exception.getMessage()));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }
}
