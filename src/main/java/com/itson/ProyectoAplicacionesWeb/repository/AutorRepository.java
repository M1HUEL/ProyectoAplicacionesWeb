
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio encargado de la persistencia
 * de los autores de Librería Raudskinna.
 *
 * Proporciona las operaciones básicas para
 * consultar, registrar y actualizar autores
 * mediante Spring Data JPA.
 *
 * Las reglas de negocio se implementarán
 * en la capa Service.
 *
 */
@Repository
public interface AutorRepository extends JpaRepository<Autor, Long> {

    /**
     * Comprueba si ya existe un autor con el nombre indicado,
     * sin distinguir entre mayúsculas y minúsculas.
     *
     * Se utilizará para evitar registros duplicados
     * durante el alta administrativa de autores.
     *
     * @param nombre nombre del autor que se desea comprobar.
     * @return true si existe un autor con ese nombre.
     *
     */
    boolean existsByNombreIgnoreCase(String nombre);

}
