
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Autor;
import com.itson.ProyectoAplicacionesWeb.repository.AutorRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

    @Mock
    private LibroRepository libroRepository;

    /** Servicio que se someterá a las pruebas. */
    private AutorService autorService;

    /**
     * Prepara el servicio antes de cada prueba.
     */
    @BeforeEach
    void configurar() {
        autorService = new AutorService(autorRepository, libroRepository);
    }

    /**
     * Comprueba que un autor válido se registra
     * utilizando el repositorio correspondiente.
     */
    @Test
    void registrar_conNombreValido_debeGuardarAutor() {

        // Ejecuta el registro.
        autorService.registrar("  Bram Stoker  ");

        // Captura la entidad enviada al repositorio.
        ArgumentCaptor<Autor> captor =
                ArgumentCaptor.forClass(Autor.class);

        verify(autorRepository).save(captor.capture());

        // Comprueba que se eliminaron los espacios exteriores.
        assertEquals("Bram Stoker", captor.getValue().getNombre());
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
     * Comprueba que el servicio permita registrar dos autores
     * con el mismo nombre.
     *
     * Los autores se identifican mediante su ID,
     * no mediante la exclusividad del nombre.
     */
    @Test
    void registrar_conNombreRepetido_debePermitirRegistro() {

        // Registra dos autores con nombres idénticos.
        autorService.registrar("Bram Stoker");
        autorService.registrar("Bram Stoker");

        // Comprueba que ambos registros llegaron al repositorio.
        verify(autorRepository, times(2))
                .save(any(Autor.class));
    }

    /**
     * Comprueba que un autor sin libros asociados
     * pueda eliminarse correctamente.
     */
    @Test
    void eliminar_autorSinLibros_eliminaCorrectamente() {

        Long autorId = 1L;
        Autor autor = new Autor("H. P. Lovecraft");

        // Simula que el autor existe.
        when(autorRepository.findById(autorId))
                .thenReturn(Optional.of(autor));

        // Simula que no existen libros asociados.
        when(libroRepository.existsByAutores_Id(autorId))
                .thenReturn(false);

        // Ejecuta la operación.
        autorService.eliminar(autorId);

        // Comprueba que se solicitó eliminar al autor.
        verify(autorRepository).delete(autor);
    }

    /**
     * Comprueba que no se permita eliminar un autor
     * cuando tiene libros asociados.
     */
    @Test
    void eliminar_autorConLibros_rechazaEliminacion() {

        Long autorId = 1L;
        Autor autor = new Autor("H. P. Lovecraft");

        when(autorRepository.findById(autorId))
                .thenReturn(Optional.of(autor));

        // Simula que el autor participa en al menos un libro.
        when(libroRepository.existsByAutores_Id(autorId))
                .thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> autorService.eliminar(autorId)
        );

        assertEquals(
                "No se puede eliminar el autor porque tiene libros asociados.",
                excepcion.getMessage()
        );

        // Verifica que nunca se solicite eliminar el autor.
        verify(autorRepository, never()).delete(any(Autor.class));
    }

    @Test
    void actualizar_autorExistente_actualizaNombre() {

        Long autorId = 1L;
        Autor autor = new Autor("Howard Lovecraft");

        when(autorRepository.findById(autorId))
                .thenReturn(Optional.of(autor));

        when(autorRepository.save(autor))
                .thenReturn(autor);

        Autor resultado = autorService.actualizar(
                autorId, "  H. P. Lovecraft  "
        );

        assertEquals("H. P. Lovecraft", resultado.getNombre());

        verify(autorRepository).save(autor);
    }

    /**
     * Comprueba que un autor pueda actualizar su nombre
     * aunque otro autor tenga el mismo.
     *
     * Los autores se identifican mediante su ID,
     * por lo que sus nombres no necesitan ser únicos.
     */
    @Test
    void actualizar_nombreRepetido_permiteActualizacion() {

        Long autorId = 1L;
        Autor autor = new Autor("H. P. Lovecraft");

        // Simula que el autor existe.
        when(autorRepository.findById(autorId))
                .thenReturn(Optional.of(autor));

        // Simula la persistencia de los cambios.
        when(autorRepository.save(autor))
                .thenReturn(autor);

        // Actualiza el nombre del autor.
        Autor resultado = autorService.actualizar(
                autorId, "Stephen King"
        );

        // Comprueba que el cambio fue aceptado.
        assertEquals("Stephen King", resultado.getNombre());

        // Verifica que se solicitó guardar la entidad.
        verify(autorRepository).save(autor);
    }

    @Test
    void actualizar_mismoNombre_permiteActualizacion() {

        Long autorId = 1L;
        Autor autor = new Autor("H. P. Lovecraft");

        when(autorRepository.findById(autorId))
                .thenReturn(Optional.of(autor));

        when(autorRepository.save(autor))
                .thenReturn(autor);

        Autor resultado = autorService.actualizar(
                autorId, "H. P. Lovecraft"
        );

        assertEquals("H. P. Lovecraft", resultado.getNombre());

        verify(autorRepository).save(autor);
    }
}
