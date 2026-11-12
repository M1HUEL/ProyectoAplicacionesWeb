
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Editorial;
import com.itson.ProyectoAplicacionesWeb.repository.EditorialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para EditorialService.
 *
 * Utiliza Mockito para simular la persistencia,
 * sin modificar la base de datos real.
 *
 */
@ExtendWith(MockitoExtension.class)
class EditorialServiceTest {

    @Mock
    private EditorialRepository editorialRepository;

    private EditorialService editorialService;

    /**
     * Inicializa el servicio antes de cada prueba.
     */
    @BeforeEach
    void configurar() {
        editorialService = new EditorialService(editorialRepository);
    }

    /**
     * Comprueba que una editorial válida pueda registrarse
     * y que sus datos se normalicen antes de guardarse.
     */
    @Test
    void registrar_conDatosValidos_debeGuardarEditorial() {

        // Simula que la editorial no existe.
        when(editorialRepository.existsByNombreIgnoreCase("Penguin Random House"))
                .thenReturn(false);

        // Ejecuta el registro.
        editorialService.registrar(
                "  Penguin Random House  ",
                "  Ciudad de México  "
        );

        // Captura la entidad enviada al repositorio.
        ArgumentCaptor<Editorial> captor =
                ArgumentCaptor.forClass(Editorial.class);

        verify(editorialRepository).save(captor.capture());

        // Comprueba que los espacios exteriores se eliminaron.
        assertEquals(
                "Penguin Random House",
                captor.getValue().getNombre()
        );

        assertEquals(
                "Ciudad de México",
                captor.getValue().getDireccion()
        );
    }

    /**
     * Comprueba que no se permita registrar una editorial
     * con un nombre nulo, vacío o compuesto solamente por espacios.
     *
     * El servicio debe rechazar estos valores antes de
     * realizar cualquier operación sobre el repositorio.
     *
     */
    @Test
    void registrar_conNombreVacio_debeRechazarRegistro() {

        // Entradas que no representan nombres válidos.
        String[] nombresInvalidos = {
                null,
                "",
                "   "
        };

        // Comprueba que todas las entradas sean rechazadas.
        for (String nombre : nombresInvalidos) {

            IllegalArgumentException excepcion = assertThrows(
                    IllegalArgumentException.class,
                    () -> editorialService.registrar(nombre, null)
            );

            // Verifica el mensaje de validación.
            assertEquals(
                    "El nombre de la editorial es obligatorio.",
                    excepcion.getMessage()
            );
        }

        // No debe consultar ni modificar el repositorio.
        verifyNoInteractions(editorialRepository);
    }

    /**
     * Comprueba que se rechace una editorial cuyo nombre
     * supere los 150 caracteres permitidos.
     *
     * Lafardilla 2026.
     */
    @Test
    void registrar_conNombreMayorA150Caracteres_debeRechazarRegistro() {

        // Genera un nombre de 151 caracteres.
        String nombreLargo = "A".repeat(151);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> editorialService.registrar(nombreLargo, null)
        );

        // Comprueba el mensaje de validación.
        assertEquals(
                "El nombre de la editorial no puede superar los 150 caracteres.",
                excepcion.getMessage()
        );

        // El repositorio no debe intervenir.
        verifyNoInteractions(editorialRepository);
    }

    /**
     * Comprueba que se rechace una dirección que supere
     * los 255 caracteres permitidos.
     *
     */
    @Test
    void registrar_conDireccionMayorA255Caracteres_debeRechazarRegistro() {

        // Genera una dirección de 256 caracteres.
        String direccionLarga = "A".repeat(256);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> editorialService.registrar(
                        "Penguin Random House",
                        direccionLarga
                )
        );

        // Comprueba el mensaje de validación.
        assertEquals(
                "La dirección no puede superar los 255 caracteres.",
                excepcion.getMessage()
        );

        // La validación ocurre antes de consultar el repositorio.
        verifyNoInteractions(editorialRepository);
    }

    /**
     * Comprueba que no se permita registrar una editorial
     * cuyo nombre ya exista en la base de datos.
     *
     * La búsqueda ignora mayúsculas y minúsculas.
     *
     */
    @Test
    void registrar_conNombreDuplicado_debeRechazarRegistro() {

        // Simula que la editorial ya está registrada.
        when(editorialRepository.existsByNombreIgnoreCase(
                "penguin random house"
        )).thenReturn(true);

        // Intenta registrar nuevamente la editorial.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> editorialService.registrar(
                        "  penguin random house  ",
                        null
                )
        );

        // Comprueba que se informa el motivo del rechazo.
        assertEquals(
                "Ya existe una editorial registrada con ese nombre.",
                excepcion.getMessage()
        );

        // Verifica que se consultó el nombre normalizado.
        verify(editorialRepository)
                .existsByNombreIgnoreCase("penguin random house");

        // Comprueba que no se intentó guardar el duplicado.
        verify(editorialRepository, never())
                .save(any(Editorial.class));
    }

}
