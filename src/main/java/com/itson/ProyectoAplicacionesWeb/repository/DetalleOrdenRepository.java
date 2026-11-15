
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.dto.ProductoMasVendidoDTO;
import com.itson.ProyectoAplicacionesWeb.model.DetalleOrden;
import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * Calcula los libros con más unidades vendidas en un periodo,
     * sin contar las órdenes con el estado indicado (CANCELADO).
     * Suma todos los formatos de un mismo libro.
     *
     * @param limite controla cuántos libros se devuelven (Pageable).
     */
    @Query("""
            SELECT new com.itson.ProyectoAplicacionesWeb.dto.ProductoMasVendidoDTO(
                       l.id, l.titulo, SUM(d.cantidad))
            FROM DetalleOrden d
            JOIN d.formatoLibro f
            JOIN f.libro l
            JOIN d.ordenCompra o
            WHERE o.fecha >= :desde
              AND o.fecha <= :hasta
              AND o.estado <> :excluido
            GROUP BY l.id, l.titulo
            ORDER BY SUM(d.cantidad) DESC, l.titulo ASC
            """)
    List<ProductoMasVendidoDTO> listarMasVendidos(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("excluido") EstadoOrden excluido,
            Pageable limite);
}
