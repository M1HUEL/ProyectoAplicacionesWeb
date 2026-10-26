package com.itson.ProyectoAplicacionesWeb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

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

    private final AutenticacionService autenticacionService;

    public AutenticacionController(
            AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    /**
     * Muestra el panel administrativo.
     *
     * La autorización no se realiza aquí, sino en SecurityConfig,
     * donde /admin/** está restringido al rol ADMIN.
     */
    @GetMapping("/admin")
    public String inicioAdmin() {
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