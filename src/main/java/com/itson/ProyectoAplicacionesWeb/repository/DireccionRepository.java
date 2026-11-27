
package com.itson.ProyectoAplicacionesWeb.repository;

import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para consultar y persistir
 * las direcciones de los clientes.
 */
public interface DireccionRepository
        extends JpaRepository<Direccion, Long> {

    /**
     * Recupera las direcciones registradas por un usuario.
     */
    List<Direccion> findByUsuario_Id(Long usuarioId);

    /**
     * Busca una dirección específica y comprueba
     * que pertenezca al usuario indicado.
     */
    Optional<Direccion> findByIdAndUsuario_Id(
            Long direccionId,
            Long usuarioId
    );

    /**
     * Comprueba si una dirección pertenece
     * al usuario indicado.
     */
    boolean existsByIdAndUsuario_Id(
            Long direccionId,
            Long usuarioId
    );
}
