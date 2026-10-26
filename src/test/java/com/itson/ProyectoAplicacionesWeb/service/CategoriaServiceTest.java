
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Categoria;
import com.itson.ProyectoAplicacionesWeb.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de las reglas de negocio
 * implementadas en CategoriaService.
 *
 * Se utiliza un repositorio simulado (mock)
 * para evitar conexiones a MySQL.
 */
class CategoriaServiceTest {

    private CategoriaService categoriaService;
    private CategoriaRepository categoriaRepository;

    /**
     * Prepara un servicio con un repositorio simulado
     * antes de ejecutar cada prueba.
     */
    @BeforeEach
    void configurar() {
        categoriaRepository = Mockito.mock(CategoriaRepository.class);
        categoriaService = new CategoriaService(categoriaRepository);
    }

    /**
     * Comprueba que no se permita registrar
     * una categoría con nombre vacío.
     */
    @Test
    void registrarCategoriaConNombreVacioDebeFallar() {

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.registrar("")
        );

        assertEquals(
                "El nombre de la categoría es obligatorio.",
                excepcion.getMessage()
        );
    }

    /**
     * Comprueba que no se permita registrar
     * una categoría cuyo nombre supere 100 caracteres.
     */
    @Test
    void registrarCategoriaConNombreDemasiadoLargoDebeFallar() {

        String nombre = "A".repeat(101);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.registrar(nombre)
        );

        assertEquals(
                "El nombre no puede superar los 100 caracteres.",
                excepcion.getMessage()
        );
    }

    /**
     * Comprueba que el servicio impida registrar
     * una categoría cuyo nombre ya existe.
     *
     * Mockito simula la respuesta del repositorio
     * sin consultar la base de datos.
     */
    @Test
    void registrarCategoriaDuplicadaDebeFallar() {

        // Simulamos que ya existe una categoría llamada Fantasía.
        when(categoriaRepository.existsByNombreIgnoreCase(anyString()))
                .thenReturn(true);

        // Verificamos que Service rechace el registro duplicado.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.registrar("Fantasía")
        );

        assertEquals(
                "Ya existe una categoría con ese nombre.",
                excepcion.getMessage()
        );
    }

    /**
     * Comprueba que una categoría válida pueda registrarse.
     *
     * Mockito simula la persistencia para verificar que
     * CategoriaService delegue correctamente en Repository.
     */
    @Test
    void registrarCategoriaValidaDebeGuardar() {

        // Simulamos que no existe una categoría con ese nombre.
        when(categoriaRepository.existsByNombreIgnoreCase("Fantasía"))
                .thenReturn(false);

        // Simulamos el comportamiento de save().
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Ejecutamos la operación del servicio.
        Categoria categoria = categoriaService.registrar("Fantasía");

        // Comprobamos el resultado.
        assertNotNull(categoria);
        assertEquals("Fantasía", categoria.getNombre());

        // Verificamos que Service haya solicitado guardar la entidad.
        verify(categoriaRepository).save(any(Categoria.class));
    }

    /**
     * Comprueba que el servicio permita actualizar
     * el nombre de una categoría existente.
     *
     * El repositorio se simula mediante Mockito para
     * evitar modificaciones en la base de datos.
     */
    @Test
    void actualizarCategoriaExistenteDebeGuardarCambios() {

        // Creamos una categoría de prueba.
        Categoria categoria = new Categoria("Ficción");

        // Simulamos que el repositorio encuentra esa categoría.
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        // Simulamos que no existe otra categoría con el nuevo nombre.
        when(categoriaRepository.findByNombreIgnoreCase("Fantasía"))
                .thenReturn(Optional.empty());

        // Simulamos la operación de guardado.
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Actualizamos mediante la capa Service.
        Categoria actualizada = categoriaService.actualizar(1L, "Fantasía");

        // Verificamos que se haya actualizado el nombre.
        assertEquals("Fantasía", actualizada.getNombre());

        // Comprobamos que se haya solicitado guardar el cambio.
        verify(categoriaRepository).save(categoria);
    }

    /**
     * Comprueba que no sea posible actualizar una categoría
     * cuyo identificador no existe en el repositorio.
     *
     * La validación corresponde a la capa Service.
     */
    @Test
    void actualizarCategoriaInexistenteDebeFallar() {

        // Simulamos que la categoría no existe.
        when(categoriaRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Verificamos que Service rechace la actualización.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.actualizar(999L, "Terror")
        );

        assertEquals(
                "No se encontró la categoría solicitada.",
                excepcion.getMessage()
        );

        // Comprobamos que no se intente guardar información.
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    /**
     * Comprueba que no se pueda actualizar una categoría
     * utilizando el nombre de otra categoría existente.
     *
     * La validación de duplicados corresponde a Service.
     */
    @Test
    void actualizarCategoriaConNombreDuplicadoDebeFallar() {

        // Categoría que intentamos modificar.
        Categoria categoria = new Categoria("Ficción");

        // Otra categoría que ya utiliza el nombre solicitado.
        Categoria categoriaExistente = Mockito.mock(Categoria.class);

        when(categoriaExistente.getId()).thenReturn(2L);

        // Simulamos las consultas al repositorio.
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(categoriaRepository.findByNombreIgnoreCase("Fantasía"))
                .thenReturn(Optional.of(categoriaExistente));

        // Comprobamos que el servicio rechace el nombre duplicado.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.actualizar(1L, "Fantasía")
        );

        assertEquals(
                "Ya existe otra categoría con ese nombre.",
                excepcion.getMessage()
        );

        // No debe solicitarse ningún guardado.
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

}
