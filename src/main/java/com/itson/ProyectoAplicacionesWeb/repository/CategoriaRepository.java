
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de persistencia para las categorías
 * de Librería Raudskinna.
 *
 * Spring Data JPA proporciona automáticamente las
 * operaciones básicas de consulta y persistencia.
 */
public interface CategoriaRepository
        extends JpaRepository<Categoria, Long> {

    /**
     * Busca una categoría por su nombre, sin distinguir
     * entre mayúsculas y minúsculas.
     *
     * @param nombre nombre de la categoría
     * @return categoría encontrada, si existe
     */
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    /**
     * Comprueba si existe una categoría con el nombre indicado.
     *
     * @param nombre nombre que se desea verificar
     * @return true cuando existe una coincidencia
     */
    boolean existsByNombreIgnoreCase(String nombre);
}
