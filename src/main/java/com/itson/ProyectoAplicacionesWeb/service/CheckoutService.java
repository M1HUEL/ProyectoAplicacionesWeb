
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import org.springframework.transaction.annotation.Transactional;
import com.itson.ProyectoAplicacionesWeb.repository.CarritoCompraRepository;
import com.itson.ProyectoAplicacionesWeb.repository.OrdenCompraRepository;
import com.itson.ProyectoAplicacionesWeb.repository.DetalleOrdenRepository;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio encargado de confirmar las compras
 * realizadas por los clientes.
 *
 * Coordinará el carrito, las órdenes y el inventario.
 */
@Service
public class CheckoutService {

    private final CarritoCompraRepository carritoCompraRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;

    public CheckoutService(
            CarritoCompraRepository carritoCompraRepository,
            OrdenCompraRepository ordenCompraRepository,
            DetalleOrdenRepository detalleOrdenRepository,
            UsuarioRepository usuarioRepository,
            InventarioService inventarioService) {

        this.carritoCompraRepository = carritoCompraRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
    }

    /**
     * Confirma la compra del cliente autenticado.
     *
     * La orden, sus detalles, los descuentos de inventario
     * y el vaciado del carrito pertenecen a una transacción.
     */
    @Transactional
    public CheckoutResponseDTO confirmarCompra(String correo) {

        // Identificar al cliente.
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no existe."
                ));

        // Recuperar su carrito.
        CarritoCompra carrito = carritoCompraRepository
                .findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente no tiene carrito de compras."
                ));

        List<ItemCarrito> articulos =
                new ArrayList<>(carrito.getItems());

        if (articulos.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se puede confirmar un carrito vacío."
            );
        }

        // Mantener un orden consistente al procesar inventarios.
        articulos.sort(Comparator.comparing(
                item -> item.getFormatoLibro().getId()
        ));

        BigDecimal total = BigDecimal.ZERO;

        // Validar artículos y calcular el total en el servidor.
        for (ItemCarrito item : articulos) {

            FormatoLibro formato = item.getFormatoLibro();

            if (item.getCantidad() <= 0) {
                throw new IllegalArgumentException(
                        "La cantidad de un artículo no es válida."
                );
            }

            if (!formato.getLibro().isActivo()) {
                throw new IllegalArgumentException(
                        "El carrito contiene un libro no disponible."
                );
            }

            BigDecimal precio = formato.getPrecio();

            if (precio == null || precio.signum() < 0) {
                throw new IllegalArgumentException(
                        "El precio de un artículo no es válido."
                );
            }

            BigDecimal subtotal = precio.multiply(
                    BigDecimal.valueOf(item.getCantidad())
            );

            total = total.add(subtotal);
        }

        // Registrar la orden.
        OrdenCompra orden = ordenCompraRepository.save(
                new OrdenCompra(usuario, total)
        );

        // Registrar los detalles y descontar inventario físico.
        for (ItemCarrito item : articulos) {

            FormatoLibro formato = item.getFormatoLibro();

            DetalleOrden detalle = new DetalleOrden(
                    orden,
                    formato,
                    item.getCantidad(),
                    formato.getPrecio()
            );

            detalleOrdenRepository.save(detalle);

            if (formato.getFormato() != Formato.DIGITAL) {
                inventarioService.descontarPorVenta(
                        formato.getId(),
                        item.getCantidad(),
                        correo
                );
            }
        }

        // Vaciar el carrito después de procesar los artículos.
        carrito.getItems().clear();

        return new CheckoutResponseDTO(
                orden.getId(),
                orden.getFecha(),
                orden.getEstado().name(),
                orden.getTotal()
        );
    }

}
