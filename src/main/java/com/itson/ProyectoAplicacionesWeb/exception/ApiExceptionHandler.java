
package com.itson.ProyectoAplicacionesWeb.exception;

import com.itson.ProyectoAplicacionesWeb.dto.ErrorRespuestaDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

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
     * Maneja los argumentos inválidos recibidos por la API.
     *
     * Devuelve HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarArgumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        HttpStatus estado = HttpStatus.BAD_REQUEST;

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

}
