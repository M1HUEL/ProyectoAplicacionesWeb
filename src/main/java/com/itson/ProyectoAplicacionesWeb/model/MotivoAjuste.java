package com.itson.ProyectoAplicacionesWeb.model;

public enum MotivoAjuste {
    REPOSICION("Reposición de stock"),


    MERMA("Merma"),


    CORRECCION("Corrección de inventario");

    private final String etiqueta;

    MotivoAjuste(String etiqueta) {
        this.etiqueta = etiqueta;
    }


    public String getEtiqueta() {
        return etiqueta;
    }
}
