
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
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
