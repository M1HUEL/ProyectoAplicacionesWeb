
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.EstadoPago;
import com.itson.ProyectoAplicacionesWeb.model.MetodoPago;
import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import com.itson.ProyectoAplicacionesWeb.model.Pago;
import com.itson.ProyectoAplicacionesWeb.repository.PagoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gestiona el registro de pagos de Librería Raudskinna.
 *
 * No procesa cobros bancarios reales.
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    /**
     * Registra un pago pendiente asociado a una orden.
     *
     * Participa obligatoriamente en una transacción
     * existente para mantener la integridad del checkout.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Pago registrarPago(
            OrdenCompra orden,
            MetodoPago metodo) {

        if (orden == null || orden.getId() == null) {
            throw new IllegalArgumentException(
                    "La orden debe estar registrada."
            );
        }

        if (metodo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un método de pago."
            );
        }

        BigDecimal monto = orden.getTotal();

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto del pago debe ser mayor que cero."
            );
        }

        if (pagoRepository.existsByOrdenCompraId(orden.getId())) {
            throw new IllegalArgumentException(
                    "La orden ya tiene un registro de pago."
            );
        }

        Pago pago = new Pago(
                orden,
                monto,
                metodo,
                EstadoPago.PENDIENTE,
                LocalDateTime.now()
        );

        return pagoRepository.save(pago);
    }
}
