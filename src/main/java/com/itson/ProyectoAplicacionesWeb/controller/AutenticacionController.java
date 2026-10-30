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

    // Servicio utilizado por la autenticación y el registro.
    private final AutenticacionService autenticacionService;

    /**
     * Inyecta el servicio necesario para gestionar
     * la autenticación y el registro de clientes.
     *
     * El panel administrativo (/admin) vive ahora
     * en PanelAdministrativoController.
     */
    public AutenticacionController(
            AutenticacionService autenticacionService) {

        this.autenticacionService = autenticacionService;
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