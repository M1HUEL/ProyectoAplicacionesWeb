package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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


}