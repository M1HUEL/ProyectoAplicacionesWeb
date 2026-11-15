
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Autor;
import com.itson.ProyectoAplicacionesWeb.repository.AutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para las operaciones de AutorService.
 *
 * Se utiliza Mockito para simular AutorRepository,
 * sin modificar los registros reales de MySQL.
 *
 */
@ExtendWith(MockitoExtension.class)
class AutorServiceTest {

    /** Repositorio simulado para las pruebas. */
    @Mock
    private AutorRepository autorRepository;

    /** Servicio que se someterá a las pruebas. */
    private AutorService autorService;

    /**
     * Prepara el servicio antes de cada prueba.
     */
    @BeforeEach
    void configurar() {
        autorService = new AutorService(autorRepository);
    }

    /**
     * Comprueba que un autor válido se registra
     * utilizando el repositorio correspondiente.
     */
    @Test
    void registrar_conNombreValido_debeGuardarAutor() {

        // Simula que todavía no existe el autor.
        when(autorRepository.existsByNombreIgnoreCase("Bram Stoker"))
                .thenReturn(false);

        // Ejecuta el registro.
        autorService.registrar("  Bram Stoker  ");

        // Captura la entidad enviada al repositorio.
        ArgumentCaptor<Autor> captor =
                ArgumentCaptor.forClass(Autor.class);

        verify(autorRepository).save(captor.capture());

        // Comprueba que se eliminaron los espacios exteriores.
        assertEquals("Bram Stoker", captor.getValue().getNombre());

        // Comprueba que se verificó el nombre normalizado.
        verify(autorRepository)
                .existsByNombreIgnoreCase("Bram Stoker");
    }

    /**
     * Comprueba que no se permita registrar un autor
     * cuyo nombre sea nulo, vacío o contenga solamente espacios.
     *
     * En estos casos no debe consultarse ni modificarse
     * el repositorio de autores.
     *
     */
    @Test
    void registrar_conNombreVacio_debeRechazarRegistro() {

        // Distintas entradas que no representan un nombre válido.
        String[] nombresInvalidos = {
                null,
                "",
                "   "
        };

        // Comprueba que todas las entradas sean rechazadas.
        for (String nombre : nombresInvalidos) {

            IllegalArgumentException excepcion = assertThrows(
                    IllegalArgumentException.class,
                    () -> autorService.registrar(nombre)
            );

            // Verifica que se informa el motivo del rechazo.
            assertEquals(
                    "El nombre del autor es obligatorio.",
                    excepcion.getMessage()
            );
        }

        // No debe realizarse ninguna operación sobre MySQL.
        verifyNoInteractions(autorRepository);
    }

    /**
     * Comprueba que no se permita registrar un autor
     * cuyo nombre supere los 150 caracteres.
     *
     * El servicio debe rechazarlo antes de acceder
     * al repositorio.
     *
     */
    @Test
    void registrar_conNombreMayorA150Caracteres_debeRechazarRegistro() {

        // Genera un nombre de 151 caracteres.
        String nombreLargo = "A".repeat(151);

        // Comprueba que el servicio rechaza el registro.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> autorService.registrar(nombreLargo)
        );

        // Verifica que se informa la restricción de longitud.
        assertEquals(
                "El nombre del autor no puede superar los 150 caracteres.",
                excepcion.getMessage()
        );

        // No debe consultar ni modificar el repositorio.
        verifyNoInteractions(autorRepository);
    }

    /**
     * Comprueba que no se permita registrar un autor
     * cuyo nombre ya esté registrado.
     *
     * El repositorio verifica la existencia sin distinguir
     * entre mayúsculas y minúsculas.
     *
     */
    @Test
    void registrar_conNombreDuplicado_debeRechazarRegistro() {

        // Simula que el autor ya existe en la base de datos.
        when(autorRepository.existsByNombreIgnoreCase("bram stoker"))
                .thenReturn(true);

        // Intenta registrar el mismo autor con otro uso de mayúsculas.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> autorService.registrar("  bram stoker  ")
        );

        // Comprueba que el servicio informa del duplicado.
        assertEquals(
                "Ya existe un autor registrado con ese nombre.",
                excepcion.getMessage()
        );

        // Verifica que se consultó el nombre sin espacios exteriores.
        verify(autorRepository)
                .existsByNombreIgnoreCase("bram stoker");

        // Comprueba que no se intentó guardar al autor duplicado.
        verify(autorRepository, never()).save(any(Autor.class));
    }

}
