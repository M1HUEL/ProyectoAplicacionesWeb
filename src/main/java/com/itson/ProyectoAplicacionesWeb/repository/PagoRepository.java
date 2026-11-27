
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de registros de pago.
 *
 * Las validaciones y reglas de negocio
 * pertenecen a la capa Service.
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    /**
     * Busca el pago asociado a una orden de compra.
     */
    Optional<Pago> findByOrdenCompraId(Long ordenCompraId);

    /**
     * Comprueba si una orden ya tiene un registro de pago.
     */
    boolean existsByOrdenCompraId(Long ordenCompraId);
}
