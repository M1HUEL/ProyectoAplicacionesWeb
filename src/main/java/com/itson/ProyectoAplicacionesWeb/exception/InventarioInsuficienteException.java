
package com.itson.ProyectoAplicacionesWeb.exception;

/**
 * Indica que una compra no puede completarse
 * porque las existencias disponibles son
 * inferiores a la cantidad solicitada.
 *
 * Esta excepción representa un conflicto
 * con el estado actual del inventario.
 */
public class InventarioInsuficienteException
        extends IllegalArgumentException {

    public InventarioInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
