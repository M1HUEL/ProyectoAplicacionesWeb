
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio encargado de la persistencia de las
 * presentaciones comerciales de los libros.
 *
 * Permite consultar los formatos y sus respectivos
 * precios mediante Spring Data JPA.
 *
 */
@Repository
public interface FormatoLibroRepository
        extends JpaRepository<FormatoLibro, Long> {


    /**
     * Recupera las presentaciones comerciales junto con
     * su libro y sus autores.
     *
     * EntityGraph permite cargar estas relaciones en la
     * misma operación de consulta, evitando accesos
     * diferidos innecesarios al construir el listado.
     *
     * @return lista de presentaciones comerciales.
     */
    @EntityGraph(attributePaths = {"libro", "libro.autores"})
    @Query("SELECT DISTINCT f FROM FormatoLibro f")
    List<FormatoLibro> listarConLibroYAutores();

    /**
     * Recupera todas las presentaciones comerciales
     * asociadas a un libro.
     *
     * Se utilizará para consultar los formatos
     * disponibles durante la administración del catálogo.
     *
     * @param libroId identificador del libro.
     * @return lista de presentaciones comerciales.
     */
    List<FormatoLibro> findByLibroId(Long libroId);

    /**
     * Recupera las presentaciones comerciales de todos
     * los libros activos del catálogo público.
     *
     * JOIN FETCH permite recuperar el libro asociado
     * en la misma consulta y evita accesos adicionales
     * al consultar su identificador.
     *
     * No recupera presentaciones de libros inactivos.
     *
     * @return presentaciones comerciales de libros activos.
     */
    @Query("""
            SELECT f
            FROM FormatoLibro f
            JOIN FETCH f.libro l
            WHERE l.activo = true
            """)
    List<FormatoLibro> listarFormatosDeLibrosActivos();

    Optional<FormatoLibro> findByLibroIdAndFormato(
            Long libroId,
            Formato formato
    );

    /**
     * Recupera las presentaciones comerciales de los libros
     * incluidos en una página del catálogo.
     *
     * @param libroIds identificadores de los libros de la página.
     * @return presentaciones comerciales de esos libros.
     */
    @Query("""
            SELECT f
            FROM FormatoLibro f
            WHERE f.libro.id IN :libroIds
            """)
    List<FormatoLibro> listarPorLibros(
            @Param("libroIds") Collection<Long> libroIds
    );
}
