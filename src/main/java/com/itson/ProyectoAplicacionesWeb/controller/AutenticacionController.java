package com.itson.ProyectoAplicacionesWeb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;
import com.itson.ProyectoAplicacionesWeb.service.OrdenCompraService;

import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.service.AutenticacionService;

/**
 * Controlador de las vistas de autenticación y registro.
 *
 * Mantiene separadas las rutas públicas de clientes
 * y el acceso al panel administrativo de Raudskinna.
 */
@Controller
public class AutenticacionController {

    // Servicios utilizados por la autenticación y el panel administrativo.
    private final AutenticacionService autenticacionService;
    private final LibroService libroService;
    private final OrdenCompraService ordenCompraService;

    /**
     * Inyecta los servicios necesarios para gestionar
     * la autenticación y consultar los indicadores administrativos.
     *
     * Lafardilla 2026.
     */
    public AutenticacionController(
            AutenticacionService autenticacionService,
            LibroService libroService,
            OrdenCompraService ordenCompraService) {

        this.autenticacionService = autenticacionService;
        this.libroService = libroService;
        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Muestra el panel administrativo con indicadores
     * obtenidos desde la base de datos MySQL.
     *
     * La autorización se gestiona en SecurityConfig.
     *
     * Lafardilla 2026.
     */
    @GetMapping("/admin")
    public String inicioAdmin(Model model) {

        // Obtiene la cantidad real de libros registrados.
        model.addAttribute("totalLibros",
                libroService.contarLibros());

        // Obtiene la cantidad de órdenes pendientes.
        model.addAttribute("ordenesPendientes",
                ordenCompraService.contarOrdenesPendientes());

        return "panel-administrativo";
    }

    /**
     * Muestra el formulario público de registro de clientes.
     */
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    /**
     * Procesa el registro de clientes.
     *
     * AutenticacionService establece obligatoriamente el rol USER,
     * independientemente de los atributos enviados por el cliente.
     */
    @PostMapping("/registro")
    public String registrar(@ModelAttribute("usuario") Usuario usuario) {
        autenticacionService.registrar(usuario);
        return "redirect:/iniciar-sesion?registrado";
    }

    /**
     * Presenta el inicio de sesión de clientes.
     *
     * La validación de credenciales mediante POST es responsabilidad
     * de Spring Security.
     */
    @GetMapping("/iniciar-sesion")
    public String mostrarLogin() {
        return "iniciar-sesion";
    }

    /**
     * Entrada pública al login administrativo.
     *
     * Reutilizamos temporalmente la plantilla de autenticación existente.
     */
    @GetMapping("/admin/login")
    public String mostrarLoginAdmin() {
        return "iniciar-sesion";
    }
}