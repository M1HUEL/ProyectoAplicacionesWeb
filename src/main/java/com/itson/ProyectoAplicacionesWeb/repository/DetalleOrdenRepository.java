
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.DetalleOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la persistencia de los detalles
 * de las órdenes de compra.
 */
@Repository
public interface DetalleOrdenRepository
        extends JpaRepository<DetalleOrden, Long> {

    @Query("""
        SELECT d FROM DetalleOrden d
        JOIN FETCH d.formatoLibro f
        JOIN FETCH f.libro l
        WHERE d.ordenCompra.id = :ordenId
        """)
    List<DetalleOrden> listarConLibro(@Param("ordenId") Long ordenId);
}
