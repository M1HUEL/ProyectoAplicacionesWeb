
package com.itson.ProyectoAplicacionesWeb.exception;

import com.itson.ProyectoAplicacionesWeb.dto.ErrorRespuestaDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

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
     * Maneja compras que no pueden completarse
     * debido a existencias insuficientes.
     *
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(InventarioInsuficienteException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarInventarioInsuficiente(
            InventarioInsuficienteException ex,
            HttpServletRequest request) {

        HttpStatus estado = HttpStatus.CONFLICT;

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

    /**
     * Conserva el formato de error de la API para las excepciones
     * de autorización lanzadas desde los controladores.
     *
     * HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarAccesoDenegado(
            AccessDeniedException ex,
            HttpServletRequest request) {

        HttpStatus estado = HttpStatus.FORBIDDEN;

        ErrorRespuestaDTO respuesta = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                "No tienes permisos para acceder a este recurso.",
                request.getRequestURI()
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

    /**
     * Garantiza que cualquier excepción no prevista responda
     * con el mismo formato JSON que el resto de la API.
     *
     * HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarErrorInesperado(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Error inesperado al procesar {}",
                request.getRequestURI(),
                ex
        );

        HttpStatus estado = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorRespuestaDTO respuesta = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.",
                request.getRequestURI()
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

}
