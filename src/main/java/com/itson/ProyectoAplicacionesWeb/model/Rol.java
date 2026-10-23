package com.itson.ProyectoAplicacionesWeb.model;

public enum Rol {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
