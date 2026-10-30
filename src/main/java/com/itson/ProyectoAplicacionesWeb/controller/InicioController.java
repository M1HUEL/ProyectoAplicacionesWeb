package com.itson.ProyectoAplicacionesWeb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la página principal de Librería Raudskinna.
 *
 * Separamos el inicio público del panel administrativo para mantener
 * organizadas las responsabilidades de la aplicación.
 */
@Controller
public class InicioController {

    /**
     * Presenta la página principal de la librería.
     *
     * Esta ruta será pública para permitir que cualquier visitante
     * pueda conocer Raudskinna sin iniciar sesión.
     */
    @GetMapping("/")
    public String mostrarInicio() {
        return "index";
    }
}