
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Categoria;
import com.itson.ProyectoAplicacionesWeb.model.Editorial;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.Libro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de las consultas JPA
 * de presentaciones comerciales de Librería Raudskinna.
 *
 * Utiliza H2 en memoria para comprobar las consultas
 * sin modificar los registros de MySQL.
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect="
                + "org.hibernate.dialect.H2Dialect",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class FormatoLibroRepositoryTest {

    @Autowired
    private FormatoLibroRepository formatoLibroRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EditorialRepository editorialRepository;

    /**
     * Comprueba que la consulta pública recupere únicamente
     * presentaciones comerciales pertenecientes a libros activos.
     *
     * También verifica que la relación FormatoLibro-Libro
     * se encuentre disponible en los resultados.
     */
    @Test
    void listarFormatosDeLibrosActivos_debeExcluirLibrosInactivos() {

        // ARRANGE: crear las relaciones obligatorias.
        Categoria categoria = categoriaRepository.save(
                new Categoria("Terror")
        );

        Editorial editorial = editorialRepository.save(
                new Editorial(
                        "Editorial de prueba",
                        "Ciudad Obregón"
                )
        );

        // Crear y guardar un libro activo.
        Libro libroActivo = new Libro(
                "La llamada de Cthulhu",
                "9780000000001",
                categoria,
                editorial
        );

        libroActivo = libroRepository.saveAndFlush(libroActivo);

        // Crear un segundo libro y desactivarlo.
        Libro libroInactivo = new Libro(
                "Libro oculto",
                "9780000000002",
                categoria,
                editorial
        );

        libroInactivo.setActivo(false);

        libroInactivo = libroRepository.saveAndFlush(libroInactivo);

        // Registrar una presentación para cada libro.
        FormatoLibro formatoActivo = formatoLibroRepository.save(
                new FormatoLibro(
                        libroActivo,
                        Formato.PASTA_DURA,
                        new BigDecimal("350.00")
                )
        );

        FormatoLibro formatoInactivo = formatoLibroRepository.save(
                new FormatoLibro(
                        libroInactivo,
                        Formato.DIGITAL,
                        new BigDecimal("149.00")
                )
        );

        formatoLibroRepository.flush();

        // ACT: ejecutar la consulta JPQL real.
        List<FormatoLibro> resultado =
                formatoLibroRepository.listarFormatosDeLibrosActivos();

        // ASSERT: solamente debe recuperarse el formato activo.
        assertEquals(1, resultado.size());

        FormatoLibro recuperado = resultado.get(0);

        assertEquals(formatoActivo.getId(), recuperado.getId());
        assertEquals(libroActivo.getId(), recuperado.getLibro().getId());
        assertEquals(Formato.PASTA_DURA, recuperado.getFormato());
        assertEquals(
                0,
                new BigDecimal("350.00").compareTo(recuperado.getPrecio())
        );

        // La presentación del libro inactivo no debe aparecer.
        assertTrue(resultado.stream().noneMatch(
                formato -> formato.getId().equals(formatoInactivo.getId())
        ));
    }
}
