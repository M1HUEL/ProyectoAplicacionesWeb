
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CarritoResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.CheckoutResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.DireccionRequestDTO;
import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.model.MetodoPago;
import com.itson.ProyectoAplicacionesWeb.service.CarritoService;
import com.itson.ProyectoAplicacionesWeb.service.CheckoutService;
import com.itson.ProyectoAplicacionesWeb.service.DireccionService;

import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador MVC encargado de presentar y confirmar
 * las compras de Librería Raudskinna.
 *
 * Reutiliza los servicios existentes para consultar
 * el carrito, gestionar direcciones y generar órdenes.
 *
 * La identidad del cliente se obtiene exclusivamente
 * del contexto de seguridad de Spring Security.
 *
 * Las reglas de negocio y las operaciones transaccionales
 * permanecen en la capa Service.
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CarritoService carritoService;
    private final CheckoutService checkoutService;
    private final DireccionService direccionService;
    private final OrdenCompraService ordenCompraService;

    /**
     * Constructor para la inyección de dependencias.
     */
    public CheckoutController(
            CarritoService carritoService,
            CheckoutService checkoutService,
            DireccionService direccionService,
            OrdenCompraService ordenCompraService) {

        this.carritoService = carritoService;
        this.checkoutService = checkoutService;
        this.direccionService = direccionService;
        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Presenta los datos necesarios para confirmar
     * la compra del cliente autenticado.
     *
     * GET /checkout
     *
     * Si el carrito está vacío, el cliente regresa
     * a la pantalla del carrito de compras.
     *
     * CheckoutService realizará nuevamente las
     * validaciones definitivas al confirmar la orden.
     */
    @GetMapping
    public String mostrarCheckout(
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        String correo = authentication.getName();

        // Recuperar el contenido actual del carrito.
        CarritoResponseDTO carrito =
                carritoService.consultarCarrito(correo);

        // Evitar presentar el formulario de compra vacío.
        if (carrito.items().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se puede confirmar un carrito vacío."
            );

            return "redirect:/carrito";
        }

        // Proporcionar los datos comerciales a Thymeleaf.
        model.addAttribute("carrito", carrito);

        // Recuperar exclusivamente las direcciones propias.
        model.addAttribute(
                "direcciones",
                direccionService.listarPorUsuario(correo)
        );

        // Proporcionar los métodos de pago admitidos.
        model.addAttribute(
                "metodosPago",
                MetodoPago.values()
        );

        return "checkout";
    }

    /**
     * Registra una dirección de envío durante el checkout.
     *
     * POST /checkout/direcciones
     *
     * DireccionService valida los datos y establece
     * como propietario al cliente autenticado.
     *
     * El registro de la dirección no confirma la compra
     * ni modifica las existencias del inventario.
     */
    @PostMapping("/direcciones")
    public String registrarDireccion(
            Authentication authentication,
            @ModelAttribute DireccionRequestDTO datos,
            RedirectAttributes redirectAttributes) {

        String correo = authentication.getName();

        try {

            // Delegar el registro al servicio existente.
            Direccion direccion =
                    direccionService.registrar(correo, datos);

            // Permitir seleccionar la dirección recién registrada.
            redirectAttributes.addFlashAttribute(
                    "direccionSeleccionadaId",
                    direccion.getId()
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La dirección se registró correctamente."
            );

        } catch (IllegalArgumentException excepcion) {

            // Conservar los datos para facilitar su corrección.
            redirectAttributes.addFlashAttribute(
                    "datosDireccion",
                    datos
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getMessage()
            );
        }

        // Aplicar el patrón Post/Redirect/Get.
        return "redirect:/checkout";
    }

    /**
     * Confirma la compra del cliente autenticado.
     *
     * POST /checkout/confirmar
     *
     * El servicio se encarga de validar la dirección,
     * las existencias y el método de pago, así como
     * de generar la orden dentro de una transacción.
     *
     * No se reciben importes ni identificadores de
     * usuarios proporcionados por el navegador.
     */
    @PostMapping("/confirmar")
    public String confirmarCompra(
            Authentication authentication,
            @RequestParam(
                    name = "direccionId",
                    required = false
            ) Long direccionId,
            @RequestParam(
                    name = "metodoPago",
                    required = false
            ) MetodoPago metodoPago,
            RedirectAttributes redirectAttributes) {

        String correo = authentication.getName();

        try {

            // Ejecutar el procesamiento transaccional.
            CheckoutResponseDTO respuesta =
                    checkoutService.confirmarCompra(
                            correo,
                            direccionId,
                            metodoPago
                    );

            // Redirigir hacia la orden recién generada.
            return "redirect:/checkout/confirmacion/"
                    + respuesta.ordenId();

        } catch (IllegalArgumentException excepcion) {

            // Conservar las opciones seleccionadas.
            redirectAttributes.addFlashAttribute(
                    "direccionSeleccionadaId",
                    direccionId
            );

            redirectAttributes.addFlashAttribute(
                    "metodoPagoSeleccionado",
                    metodoPago
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    excepcion.getMessage()
            );

            return "redirect:/checkout";
        }
    }

    /**
     * Presenta la confirmación de una orden registrada.
     *
     * GET /checkout/confirmacion/{ordenId}
     *
     * Recupera los datos persistidos de la orden,
     * comprobando que pertenezca al cliente autenticado.
     *
     * Esta operación es exclusivamente de consulta.
     * Actualizar la página no vuelve a procesar la compra,
     * registrar el pago ni descontar inventario.
     */
    @GetMapping("/confirmacion/{ordenId}")
    public String mostrarConfirmacion(
            Authentication authentication,
            @PathVariable("ordenId") Long ordenId,
            Model model) {

        String correo = authentication.getName();

        // Recuperar la orden mediante una consulta
        // restringida al cliente autenticado.
        OrdenCompraResponseDTO orden =
                ordenCompraService.consultarOrdenPropia(
                        correo,
                        ordenId
                );

        // Proporcionar la información a Thymeleaf.
        model.addAttribute("orden", orden);

        return "orden-confirmada";
    }

}
