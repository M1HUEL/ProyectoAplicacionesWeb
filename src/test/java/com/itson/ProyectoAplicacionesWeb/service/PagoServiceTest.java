
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.PagoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoService pagoService;

    private OrdenCompra orden;

    @BeforeEach
    void configurar() {

        orden = mock(OrdenCompra.class);

        when(orden.getId()).thenReturn(10L);

        lenient().when(orden.getTotal())
                .thenReturn(new BigDecimal("499.00"));
    }

    @Test
    void registrarPago_datosValidos_creaPagoPendiente() {

        when(pagoRepository.existsByOrdenCompraId(10L))
                .thenReturn(false);

        when(pagoRepository.save(any(Pago.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0));

        Pago pago = pagoService.registrarPago(
                orden,
                MetodoPago.TRANSFERENCIA
        );

        assertNotNull(pago);
        assertEquals(orden, pago.getOrdenCompra());
        assertEquals(
                0,
                new BigDecimal("499.00")
                        .compareTo(pago.getMonto())
        );
        assertEquals(
                MetodoPago.TRANSFERENCIA,
                pago.getMetodo()
        );
        assertEquals(
                EstadoPago.PENDIENTE,
                pago.getEstado()
        );
        assertNotNull(pago.getFechaRegistro());

        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void registrarPago_metodoNulo_lanzaExcepcion() {

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(orden, null)
        );

        assertEquals(
                "Debe seleccionar un método de pago.",
                excepcion.getMessage()
        );

        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_montoInvalido_lanzaExcepcion() {

        when(orden.getTotal()).thenReturn(BigDecimal.ZERO);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(
                        orden,
                        MetodoPago.EFECTIVO
                )
        );

        assertEquals(
                "El monto del pago debe ser mayor que cero.",
                excepcion.getMessage()
        );

        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_pagoDuplicado_lanzaExcepcion() {

        when(pagoRepository.existsByOrdenCompraId(10L))
                .thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(
                        orden,
                        MetodoPago.TARJETA
                )
        );

        assertEquals(
                "La orden ya tiene un registro de pago.",
                excepcion.getMessage()
        );

        verify(pagoRepository, never()).save(any());
    }
}
