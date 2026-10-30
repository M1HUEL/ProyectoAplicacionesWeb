
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
       JOIN FETCH o.usuario u
       ORDER BY o.fecha DESC
       """)
    List<OrdenCompra> listarConUsuarios();

    /**
     * Filtra las órdenes por estado, rango de fechas y cliente
     * (nombre o correo electrónico).
     */
    @Query("""
       SELECT o
       FROM OrdenCompra o
       JOIN FETCH o.usuario u
       WHERE (:estado IS NULL OR o.estado = :estado)
         AND (:fechaDesde IS NULL OR o.fecha >= :fechaDesde)
         AND (:fechaHasta IS NULL OR o.fecha <= :fechaHasta)
         AND (:cliente IS NULL OR :cliente = '' OR 
              LOWER(u.nombre) LIKE LOWER(CONCAT('%', :cliente, '%')) OR
              LOWER(u.correo) LIKE LOWER(CONCAT('%', :cliente, '%')))
       ORDER BY o.fecha DESC
       """)
    List<OrdenCompra> filtrar(@Param("estado") EstadoOrden estado,
                              @Param("fechaDesde") LocalDateTime fechaDesde,
                              @Param("fechaHasta") LocalDateTime fechaHasta,
                              @Param("cliente") String cliente);

    /**
     * Cuenta las órdenes que tienen un estado específico.
     *
     * Spring Data JPA genera la consulta automáticamente
     * a partir del nombre del método.
     *
     * @param estado estado de las órdenes que se contarán.
     * @return cantidad de órdenes con ese estado.
     */
    long countByEstado(EstadoOrden estado);

}
