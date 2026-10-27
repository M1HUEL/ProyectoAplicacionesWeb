
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.ItemStock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio encargado de consultar y administrar
 * los registros de inventario físico de Librería Raudskinna.
 *
 * Cada registro de ItemStock está asociado a una
 * presentación comercial mediante FormatoLibro.
 *
 * Las reglas y validaciones de inventario pertenecen
 * a la capa Service.
 *
 */
@Repository
public interface ItemStockRepository
        extends JpaRepository<ItemStock, Long> {

    /**
     * Busca el registro de inventario correspondiente
     * a una presentación comercial de un libro.
     *
     * Se utiliza Optional porque los formatos digitales
     * no necesitan un registro de existencias físicas.
     *
     * @param formatoLibroId identificador de la presentación.
     * @return inventario asociado, si existe.
     */
    Optional<ItemStock> findByFormatoLibroId(Long formatoLibroId);


    /**
     * Recupera todos los registros de inventario junto
     * con la presentación comercial a la que pertenecen.
     *
     * Permite asociar las existencias con los formatos
     * sin realizar una consulta adicional por cada fila.
     *
     * @return lista de registros de inventario físico.
     */
    @EntityGraph(attributePaths = "formatoLibro")
    @Query("SELECT i FROM ItemStock i")
    List<ItemStock> listarConFormatoLibro();
}
