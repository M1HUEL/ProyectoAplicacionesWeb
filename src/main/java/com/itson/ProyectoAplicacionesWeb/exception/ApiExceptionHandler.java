
package com.itson.ProyectoAplicacionesWeb.exception;

import com.itson.ProyectoAplicacionesWeb.dto.ErrorRespuestaDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Maneja de forma centralizada los errores
 * producidos por los controladores REST de la API.
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarErrorHttp(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatus estado = HttpStatus.valueOf(ex.getStatusCode().value());

        ErrorRespuestaDTO respuesta = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                ex.getReason(),
                request.getRequestURI()
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

    /**
     * Maneja argumentos inválidos y conflictos
     * por correos electrónicos duplicados.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarArgumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        HttpStatus estado = "El correo ya está registrado".equals(ex.getMessage())
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        ErrorRespuestaDTO respuesta = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

    /**
     * Maneja parámetros cuyo tipo de dato es incorrecto.
     *
     * Por ejemplo: GET /api/libros/abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        HttpStatus estado = HttpStatus.BAD_REQUEST;

        ErrorRespuestaDTO respuesta = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                "El parámetro '" + ex.getName()
                        + "' tiene un valor no válido.",
                request.getRequestURI()
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

    /**
     * Maneja los errores de validación de los DTO.
     *
     * Por ejemplo, campos vacíos, correos inválidos
     * y contraseñas demasiado cortas.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        HttpStatus estado = HttpStatus.BAD_REQUEST;

        String mensaje = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(" "));

        ErrorRespuestaDTO respuesta = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

}
