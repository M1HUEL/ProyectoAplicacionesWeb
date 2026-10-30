
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Editorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio encargado de la persistencia
 * de las editoriales de Librería Raudskinna.
 *
 * Proporciona las operaciones básicas para
 * consultar, registrar y actualizar editoriales
 * mediante Spring Data JPA.
 *
 * Las reglas de negocio se implementarán
 * en la capa Service.
 *
 */
@Repository
public interface EditorialRepository
        extends JpaRepository<Editorial, Long> {

    /**
     * Comprueba si existe una editorial con el nombre indicado,
     * sin distinguir entre mayúsculas y minúsculas.
     *
     * Se utilizará para evitar duplicados durante
     * el registro administrativo de editoriales.
     *
     * @param nombre nombre de la editorial a comprobar.
     * @return true si ya existe una editorial con ese nombre.
     *
     */
    boolean existsByNombreIgnoreCase(String nombre);


}
