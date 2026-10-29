
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import com.itson.ProyectoAplicacionesWeb.repository.OrdenCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar las órdenes de compra
 * de Librería Raudskinna.
 */
@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;

    // Inyección de dependencias mediante constructor.
    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository) {
        this.ordenCompraRepository = ordenCompraRepository;
    }


    /**
     * Recupera las órdenes junto con sus usuarios,
     * ordenadas desde la más reciente.
     */
    @Transactional(readOnly = true)
    public List<OrdenCompra> listarTodas() {
        return ordenCompraRepository.listarConUsuarios();
    }

    /**
     * Busca una orden de compra mediante su identificador.
     *
     * @param id identificador de la orden.
     * @return orden encontrada.
     */
    @Transactional(readOnly = true)
    public OrdenCompra buscarPorId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la orden no es válido."
            );
        }

        return ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una orden con el identificador indicado."
                ));
    }

    /**
     * Actualiza el estado de una orden respetando
     * las transiciones permitidas.
     */
    @Transactional
    public OrdenCompra actualizarEstado(Long ordenId, EstadoOrden nuevoEstado) {

        // Recupera la orden existente.
        OrdenCompra orden = buscarPorId(ordenId);

        if (nuevoEstado == null) {
            throw new IllegalArgumentException(
                    "El nuevo estado de la orden es obligatorio."
            );
        }

        EstadoOrden estadoActual = orden.getEstado();

        boolean transicionValida = switch (estadoActual) {

            case PENDIENTE ->
                    nuevoEstado == EstadoOrden.PROCESANDO
                            || nuevoEstado == EstadoOrden.CANCELADO;

            case PROCESANDO ->
                    nuevoEstado == EstadoOrden.ENVIADO
                            || nuevoEstado == EstadoOrden.CANCELADO;

            case ENVIADO ->
                    nuevoEstado == EstadoOrden.ENTREGADO;

            case ENTREGADO, CANCELADO -> false;
        };

        if (!transicionValida) {
            throw new IllegalArgumentException(
                    "No se permite cambiar la orden de "
                            + estadoActual + " a " + nuevoEstado + "."
            );
        }

        // La entidad está administrada por JPA durante la transacción.
        orden.setEstado(nuevoEstado);

        return orden;
    }

}
