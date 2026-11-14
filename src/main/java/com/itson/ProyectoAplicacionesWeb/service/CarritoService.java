
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.AgregarItemCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.CarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ItemCarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ModificarCantidadCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.model.CarritoCompra;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.ItemCarrito;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.CarritoCompraRepository;
import com.itson.ProyectoAplicacionesWeb.repository.FormatoLibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemCarritoRepository;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de gestionar el carrito
 * de compras de los clientes.
 */
@Service
public class CarritoService {

    private final CarritoCompraRepository carritoCompraRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final FormatoLibroRepository formatoLibroRepository;

    public CarritoService(
            CarritoCompraRepository carritoCompraRepository,
            UsuarioRepository usuarioRepository,
            ItemCarritoRepository itemCarritoRepository,
            FormatoLibroRepository formatoLibroRepository) {

        this.carritoCompraRepository = carritoCompraRepository;
        this.usuarioRepository = usuarioRepository;
        this.itemCarritoRepository = itemCarritoRepository;
        this.formatoLibroRepository = formatoLibroRepository;
    }

    /**
     * Busca el carrito perteneciente al cliente autenticado.
     *
     * La identidad del cliente se obtiene de su JWT validado.
     */
    @Transactional(readOnly = true)
    public Optional<CarritoCompra> buscarPorCorreo(String correo) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no existe."
                ));

        return carritoCompraRepository.findByUsuarioId(usuario.getId());
    }

    /**
     * Consulta el contenido del carrito de un cliente.
     *
     * Los precios y subtotales se calculan en el servidor.
     */
    @Transactional(readOnly = true)
    public CarritoResponseDTO consultarCarrito(String correo) {

        Optional<CarritoCompra> carritoEncontrado = buscarPorCorreo(correo);

        // Un cliente nuevo puede no tener carrito todavía.
        if (carritoEncontrado.isEmpty()) {
            return new CarritoResponseDTO(
                    null,
                    List.of(),
                    BigDecimal.ZERO
            );
        }

        CarritoCompra carrito = carritoEncontrado.get();

        List<ItemCarritoResponseDTO> items = carrito.getItems()
                .stream()
                .map(item -> {

                    BigDecimal precio = item.getFormatoLibro().getPrecio();

                    BigDecimal subtotal = precio.multiply(
                            BigDecimal.valueOf(item.getCantidad())
                    );

                    return new ItemCarritoResponseDTO(
                            item.getFormatoLibro().getId(),
                            item.getFormatoLibro().getLibro().getTitulo(),
                            item.getFormatoLibro().getFormato().name(),
                            item.getCantidad(),
                            precio,
                            subtotal
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(ItemCarritoResponseDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarritoResponseDTO(
                carrito.getId(),
                items,
                total
        );
    }

    /**
     * Agrega una presentación comercial al carrito del cliente.
     *
     * Si la presentación ya está en el carrito,
     * incrementa la cantidad existente.
     */
    @Transactional
    public CarritoResponseDTO agregarItem(
            String correo,
            AgregarItemCarritoRequestDTO solicitud) {

        if (solicitud == null
                || solicitud.formatoLibroId() == null
                || solicitud.formatoLibroId() <= 0
                || solicitud.cantidad() == null
                || solicitud.cantidad() <= 0) {

            throw new IllegalArgumentException(
                    "Los datos del artículo no son válidos."
            );
        }

        // Identificar al cliente autenticado.
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no existe."
                ));

        // Verificar que la presentación comercial exista.
        FormatoLibro formatoLibro = formatoLibroRepository
                .findById(solicitud.formatoLibroId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La presentación comercial solicitada no existe."
                ));

        // Recuperar el carrito o crear uno nuevo.
        CarritoCompra carrito = carritoCompraRepository
                .findByUsuarioId(usuario.getId())
                .orElseGet(() -> carritoCompraRepository.save(
                        new CarritoCompra(usuario)
                ));

        // Buscar si la presentación ya está agregada.
        Optional<ItemCarrito> itemExistente = itemCarritoRepository
                .findByCarritoCompraIdAndFormatoLibroId(
                        carrito.getId(),
                        formatoLibro.getId()
                );

        if (itemExistente.isPresent()) {

            ItemCarrito item = itemExistente.get();

            long cantidadNueva =
                    (long) item.getCantidad() + solicitud.cantidad();

            if (cantidadNueva > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "La cantidad solicitada supera el límite permitido."
                );
            }

            item.setCantidad((int) cantidadNueva);

            itemCarritoRepository.save(item);

        } else {

            ItemCarrito nuevoItem = new ItemCarrito(
                    carrito,
                    formatoLibro,
                    solicitud.cantidad()
            );

            itemCarritoRepository.save(nuevoItem);

            // Sincronizar la colección en memoria con el nuevo artículo.
            carrito.getItems().add(nuevoItem);
        }

        return consultarCarrito(correo);
    }

    /**
     * Establece una nueva cantidad para un artículo
     * que ya pertenece al carrito del cliente.
     */
    @Transactional
    public CarritoResponseDTO modificarCantidad(
            String correo,
            Long formatoLibroId,
            ModificarCantidadCarritoRequestDTO solicitud) {

        // Validar los datos recibidos.
        if (formatoLibroId == null || formatoLibroId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la presentación no es válido."
            );
        }

        if (solicitud == null
                || solicitud.cantidad() == null
                || solicitud.cantidad() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        // Buscar exclusivamente el carrito del cliente autenticado.
        CarritoCompra carrito = buscarPorCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El cliente todavía no tiene carrito."
                ));

        // Comprobar que el artículo pertenece a ese carrito.
        ItemCarrito item = itemCarritoRepository
                .findByCarritoCompraIdAndFormatoLibroId(
                        carrito.getId(),
                        formatoLibroId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El artículo no existe en el carrito."
                ));

        // Reemplazar la cantidad anterior.
        item.setCantidad(solicitud.cantidad());

        // JPA detectará el cambio dentro de la transacción.
        return consultarCarrito(correo);
    }

    /**
     * Elimina una presentación comercial del carrito
     * perteneciente al cliente autenticado.
     */
    @Transactional
    public CarritoResponseDTO eliminarItem(
            String correo,
            Long formatoLibroId) {

        if (formatoLibroId == null || formatoLibroId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la presentación no es válido."
            );
        }

        // Recuperar exclusivamente el carrito del cliente.
        CarritoCompra carrito = buscarPorCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El cliente todavía no tiene carrito."
                ));

        // Buscar el artículo dentro de ese carrito.
        ItemCarrito item = itemCarritoRepository
                .findByCarritoCompraIdAndFormatoLibroId(
                        carrito.getId(),
                        formatoLibroId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El artículo no existe en el carrito."
                ));

        // Eliminar el artículo de la colección del carrito.
        carrito.getItems().removeIf(
                articulo -> articulo.getId().equals(item.getId())
        );

        // orphanRemoval=true eliminará el registro asociado.
        return consultarCarrito(correo);
    }

}
