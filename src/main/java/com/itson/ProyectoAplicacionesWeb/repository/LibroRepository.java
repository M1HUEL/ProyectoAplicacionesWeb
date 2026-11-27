package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {

    /**
     * Comprueba si ya existe un libro registrado
     * con el ISBN indicado.
     *
     * Se utilizará desde LibroService para evitar
     * registros duplicados.
     *
     * @param isbn identificador ISBN que se desea consultar.
     * @return true si el ISBN ya está registrado.
     *
     */
    boolean existsByIsbn(String isbn);

    /**
     * Recupera los libros visibles para el catálogo público.
     *
     * Spring Data JPA construye la consulta a partir
     * del nombre del método.
     *
     * @return libros que tienen activo = true.
     */
    List<Libro> findByActivoTrue();

    /**
     * Recupera una página del catálogo público aplicando,
     * cuando se proporcionan, la búsqueda por título o autor
     * y el filtro por categoría.
     *
     * El filtrado y la paginación se resuelven en la base de
     * datos para no cargar el catálogo completo en memoria.
     *
     * @param termino texto a buscar en título o autores.
     * @param categoria nombre de la categoría seleccionada.
     * @param pageable página y orden solicitados.
     * @return página de libros activos que coinciden.
     */
    @Query(value = """
            SELECT DISTINCT l
            FROM Libro l
            LEFT JOIN l.autores a
            WHERE l.activo = true
              AND (:termino IS NULL
                   OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :termino, '%'))
                   OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
              AND (:categoria IS NULL
                   OR LOWER(l.categoria.nombre) = LOWER(:categoria))
            """, countQuery = """
            SELECT COUNT(DISTINCT l)
            FROM Libro l
            LEFT JOIN l.autores a
            WHERE l.activo = true
              AND (:termino IS NULL
                   OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :termino, '%'))
                   OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
              AND (:categoria IS NULL
                   OR LOWER(l.categoria.nombre) = LOWER(:categoria))
            """)
    Page<Libro> buscarCatalogoPublico(
            @Param("termino") String termino,
            @Param("categoria") String categoria,
            Pageable pageable);

    /**
     * Busca un libro visible en el catálogo público.
     *
     * Evita recuperar libros inactivos, aunque se
     * conozca su identificador.
     *
     * @param id identificador del libro.
     * @return libro activo, si existe.
     */
    Optional<Libro> findByIdAndActivoTrue(Long id);

    /**
     * Comprueba si existen libros asociados a un autor.
     *
     * Se utiliza para evitar eliminar autores que
     * todavía participan en libros registrados.
     *
     * @param autorId identificador del autor.
     * @return true si existe al menos un libro asociado.
     */
    boolean existsByAutores_Id(Long autorId);

    /**
     * Comprueba si existen libros asociados a una editorial.
     *
     * Se utiliza para impedir eliminar editoriales que
     * todavía tienen libros registrados en el catálogo.
     *
     * Incluye tanto libros activos como inactivos,
     * preservando las relaciones existentes.
     *
     * @param editorialId identificador de la editorial.
     * @return true si existe al menos un libro asociado.
     */
    boolean existsByEditorial_Id(Long editorialId);

    Optional<Libro> findByIsbn(String isbn);
}
