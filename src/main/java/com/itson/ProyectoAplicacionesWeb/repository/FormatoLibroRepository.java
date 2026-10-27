
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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


}
