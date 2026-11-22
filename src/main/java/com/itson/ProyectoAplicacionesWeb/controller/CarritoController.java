
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.AgregarItemCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.CarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ModificarCantidadCarritoRequestDTO;
import com.itson.ProyectoAplicacionesWeb.service.CarritoService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador MVC encargado de gestionar la interfaz
 * gráfica del carrito de compras de Librería Raudskinna.
 *
 * Las solicitudes se realizan mediante formularios HTML
 * y las vistas se renderizan utilizando Thymeleaf.
 *
 * La identidad del cliente se obtiene de la sesión
 * autenticada mediante Spring Security.
 *
 * Las reglas y validaciones de negocio pertenecen
 * exclusivamente a CarritoService.
 *
 * Las operaciones que modifican el carrito utilizan
 * el patrón Post/Redirect/Get para evitar el reenvío
 * accidental de formularios al recargar la página.
 */
@Controller
@RequestMapping("/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    /**
     * Constructor para la inyección de dependencias.
     *
     * @param carritoService servicio del carrito de compras.
     */
    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /**
     * Muestra el contenido del carrito perteneciente
     * al cliente autenticado.
     *
     * GET /carrito
     *
     * @param authentication autenticación del cliente.
     * @param model modelo de datos de Thymeleaf.
     * @return nombre de la vista del carrito.
     */
    @GetMapping
    public String mostrarCarrito(
            Authentication authentication,
            Model model) {

        // Obtener la identidad del cliente autenticado.
        String correo = authentication.getName();

        // Consultar el contenido y total del carrito.
        CarritoResponseDTO carrito =
                carritoService.consultarCarrito(correo);

        // Proporcionar los datos a la vista.
        model.addAttribute("carrito", carrito);

        return "carrito";
    }

    /**
     * Agrega una presentación comercial al carrito
     * del cliente autenticado.
     *
     * POST /carrito/agregar
     *
     * Las cantidades y la disponibilidad comercial
     * son validadas por CarritoService.
     *
     * @param authentication autenticación del cliente.
     * @param formatoLibroId presentación seleccionada.
     * @param cantidad unidades solicitadas.
     * @param redirectAttributes mensajes de la operación.
     * @return redirección hacia el carrito.
     */
    @PostMapping("/agregar")
    public String agregarItem(
            Authentication authentication,
            @RequestParam("formatoLibroId") Long formatoLibroId,
            @RequestParam("cantidad") Integer cantidad,
            RedirectAttributes redirectAttributes) {

        String correo = authentication.getName();

        try {

            // Construir la solicitud utilizando el DTO existente.
            AgregarItemCarritoRequestDTO solicitud =
                    new AgregarItemCarritoRequestDTO(
                            formatoLibroId,
                            cantidad
                    );

            // Delegar la operación al servicio.
            carritoService.agregarItem(correo, solicitud);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El artículo se agregó correctamente al carrito."
            );

        } catch (IllegalArgumentException excepcion) {

            // Informar al cliente cuando se rechace la solicitud.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getMessage()
            );
        }

        return "redirect:/carrito";
    }

    /**
     * Modifica la cantidad de una presentación comercial
     * perteneciente al carrito del cliente autenticado.
     *
     * POST /carrito/modificar
     *
     * @param authentication autenticación del cliente.
     * @param formatoLibroId presentación comercial.
     * @param cantidad nueva cantidad solicitada.
     * @param redirectAttributes mensajes de la operación.
     * @return redirección hacia el carrito.
     */
    @PostMapping("/modificar")
    public String modificarCantidad(
            Authentication authentication,
            @RequestParam("formatoLibroId") Long formatoLibroId,
            @RequestParam("cantidad") Integer cantidad,
            RedirectAttributes redirectAttributes) {

        String correo = authentication.getName();

        try {

            ModificarCantidadCarritoRequestDTO solicitud =
                    new ModificarCantidadCarritoRequestDTO(cantidad);

            carritoService.modificarCantidad(
                    correo,
                    formatoLibroId,
                    solicitud
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La cantidad se actualizó correctamente."
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getMessage()
            );

        } catch (ResponseStatusException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getReason()
            );
        }

        return "redirect:/carrito";
    }

    /**
     * Elimina una presentación comercial del carrito
     * perteneciente al cliente autenticado.
     *
     * POST /carrito/eliminar
     *
     * @param authentication autenticación del cliente.
     * @param formatoLibroId presentación que será eliminada.
     * @param redirectAttributes mensajes de la operación.
     * @return redirección hacia el carrito.
     */
    @PostMapping("/eliminar")
    public String eliminarItem(
            Authentication authentication,
            @RequestParam("formatoLibroId") Long formatoLibroId,
            RedirectAttributes redirectAttributes) {

        String correo = authentication.getName();

        try {

            carritoService.eliminarItem(
                    correo,
                    formatoLibroId
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El artículo se eliminó correctamente."
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getMessage()
            );

        } catch (ResponseStatusException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getReason()
            );
        }

        return "redirect:/carrito";
    }

    /**
     * Permite recuperar la navegación comercial después
     * de que un visitante se autentique.
     *
     * GET /carrito/continuar/{libroId}
     *
     * Esta ruta está protegida mediante Spring Security.
     *
     * No agrega artículos al carrito ni modifica datos
     * persistidos. Únicamente redirige al cliente hacia
     * el detalle del libro que estaba consultando.
     *
     * @param libroId identificador del libro consultado.
     * @return redirección al detalle del libro.
     */
    @GetMapping("/continuar/{libroId}")
    public String continuarCompra(
            @PathVariable("libroId") Long libroId) {

        return "redirect:/detalle-libro/" + libroId;
    }

}
