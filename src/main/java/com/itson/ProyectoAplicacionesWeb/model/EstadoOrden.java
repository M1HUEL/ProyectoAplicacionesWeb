package com.itson.ProyectoAplicacionesWeb.model;

/**
 * Representa los posibles estados de una orden de compra.
 */
public enum EstadoOrden {
    PENDIENTE,
    PROCESANDO,
    ENVIADO,
    ENTREGADO,
    CANCELADO
}