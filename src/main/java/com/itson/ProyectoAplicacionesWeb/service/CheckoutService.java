
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;
import org.springframework.transaction.annotation.Transactional;

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
    private final DireccionRepository direccionRepository;
    private final PagoService pagoService;

    public CheckoutService(
            CarritoCompraRepository carritoCompraRepository,
            OrdenCompraRepository ordenCompraRepository,
            DetalleOrdenRepository detalleOrdenRepository,
            UsuarioRepository usuarioRepository,
            InventarioService inventarioService,
            DireccionRepository direccionRepository,
            PagoService pagoService) {

        this.carritoCompraRepository = carritoCompraRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.direccionRepository = direccionRepository;
        this.pagoService = pagoService;
    }

    /**
     * Confirma la compra del cliente autenticado.
     *
     * La orden, sus detalles, los descuentos de inventario
     * y el vaciado del carrito pertenecen a una transacción.
     */
    @Transactional
    public CheckoutResponseDTO confirmarCompra(
            String correo,
            Long direccionId,
            MetodoPago metodoPago) {

        if (metodoPago == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar un método de pago."
            );
        }

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

        // Determinar si el carrito contiene artículos físicos.
        boolean requiereEnvio = articulos.stream()
                .anyMatch(item ->
                        item.getFormatoLibro().getFormato() != Formato.DIGITAL
                );

        // Las compras físicas o mixtas requieren una dirección.
        if (requiereEnvio && direccionId == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una dirección de envío."
            );
        }

        DireccionEnvio direccionHistorica = null;

        // Si el cliente seleccionó una dirección, comprobar
        // que exista y pertenezca al usuario autenticado.
        if (direccionId != null) {

            if (direccionId <= 0) {
                throw new IllegalArgumentException(
                        "El identificador de la dirección no es válido."
                );
            }

            Direccion direccion = direccionRepository
                    .findByIdAndUsuario_Id(direccionId, usuario.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La dirección no existe o no pertenece al cliente."
                    ));

            // Copiar los datos sin conservar una dependencia
            // hacia la dirección editable del cliente.
            if (requiereEnvio) {
                direccionHistorica = new DireccionEnvio(
                        direccion.getNombreDestinatario(),
                        direccion.getCalle(),
                        direccion.getNumeroExterior(),
                        direccion.getNumeroInterior(),
                        direccion.getColonia(),
                        direccion.getCodigoPostal(),
                        direccion.getCiudad(),
                        direccion.getEstado(),
                        direccion.getPais(),
                        direccion.getReferencias()
                );
            }
        }

        // Crear la orden.
        OrdenCompra orden = new OrdenCompra(usuario, total);

        // Incorporar la copia histórica, cuando corresponda.
        orden.setDireccionEnvio(direccionHistorica);

        // Persistir la orden después de las validaciones.
        orden = ordenCompraRepository.save(orden);

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

        // Registrar el pago como pendiente.
        // No se realiza ningún cobro bancario real.
        pagoService.registrarPago(orden, metodoPago);

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
