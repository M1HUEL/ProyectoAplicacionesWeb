
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

}
