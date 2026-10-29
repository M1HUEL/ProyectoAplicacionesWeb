
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la persistencia de órdenes de compra.
 */
@Repository
public interface OrdenCompraRepository
        extends JpaRepository<OrdenCompra, Long> {
}
