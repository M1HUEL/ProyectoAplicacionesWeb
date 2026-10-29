
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.DetalleOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la persistencia de los detalles
 * de las órdenes de compra.
 */
@Repository
public interface DetalleOrdenRepository
        extends JpaRepository<DetalleOrden, Long> {
}
