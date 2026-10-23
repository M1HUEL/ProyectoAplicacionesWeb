package com.itson.ProyectoAplicacionesWeb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.service.AutenticacionService;

@Controller
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @GetMapping("/")
    public String inicio() {
        return "panel-administrativo";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute("usuario") Usuario usuario) {
        autenticacionService.registrar(usuario);
        return "redirect:/iniciar-sesion?registrado";
    }

    @GetMapping("/iniciar-sesion")
    public String mostrarLogin() {
        return "iniciar-sesion";
    }
}
