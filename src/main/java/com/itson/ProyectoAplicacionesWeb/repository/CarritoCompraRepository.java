
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.CarritoCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para consultar y persistir
 * los carritos de compra de los clientes.
 */
public interface CarritoCompraRepository
        extends JpaRepository<CarritoCompra, Long> {

    /**
     * Busca el carrito perteneciente a un usuario.
     *
     * @param usuarioId identificador del cliente.
     * @return carrito, si existe.
     */
    Optional<CarritoCompra> findByUsuarioId(Long usuarioId);
}
