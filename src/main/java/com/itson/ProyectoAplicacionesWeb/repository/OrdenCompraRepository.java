
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la persistencia de órdenes de compra.
 */
@Repository
public interface OrdenCompraRepository
        extends JpaRepository<OrdenCompra, Long> {

    /**
     * Recupera las órdenes junto con sus usuarios
     * para el listado administrativo.
     */
    @Query("""
       SELECT o
       FROM OrdenCompra o
       JOIN FETCH o.usuario
       ORDER BY o.fecha DESC
       """)
    List<OrdenCompra> listarConUsuarios();


}
