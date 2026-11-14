
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio encargado de consultar y persistir
 * los artículos incluidos en los carritos de compra.
 */
public interface ItemCarritoRepository
        extends JpaRepository<ItemCarrito, Long> {

    /**
     * Busca una presentación comercial dentro
     * de un carrito específico.
     */
    Optional<ItemCarrito> findByCarritoCompraIdAndFormatoLibroId(
            Long carritoId,
            Long formatoLibroId
    );
}
